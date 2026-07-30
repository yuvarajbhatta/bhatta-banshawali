#!/usr/bin/env bash
set -euo pipefail

# Rehearses Flyway migrations against a throwaway copy of production data.
#
# We don't run a persistent staging environment (single shared home-server
# host, see docs/03-target-architecture.md and docs/08-implementation-roadmap.md
# for why that was judged not worth it here). Instead this script: dumps prod
# read-only, loads the dump into a disposable MySQL container, then starts the
# app against that container so Flyway runs for real, exactly as it would on
# deploy. Nothing persists after the run -- the container is removed and the
# dump is deleted even on failure.
#
# Must be run on the prod host: it needs /srv/config/familytree/application.properties
# and network access to the production database.
#
# Usage: scripts/migration-rehearsal.sh

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROD_CONFIG_FILE=/srv/config/familytree/application.properties
CONTAINER_NAME=familytree-migration-rehearsal
WORKDIR=$(mktemp -d)
chmod 700 "$WORKDIR"
DUMP_FILE="$WORKDIR/prod-dump.sql"
APP_LOG="$WORKDIR/app.log"
ROOT_PASSWORD=$(openssl rand -hex 16)
APP_PID=""

cleanup() {
  local exit_code=$?
  [[ -n "$APP_PID" ]] && kill "$APP_PID" >/dev/null 2>&1 || true
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  rm -rf "$WORKDIR"
  exit "$exit_code"
}
trap cleanup EXIT

[[ -f "$PROD_CONFIG_FILE" ]] || {
  echo "Not found: $PROD_CONFIG_FILE -- this script must run on the prod host." >&2
  exit 1
}

echo "==> Reading production datasource config from $PROD_CONFIG_FILE"
read_prop() {
  sed -n "s/^$1=//p" "$PROD_CONFIG_FILE" | head -1
}
SPRING_DATASOURCE_URL=$(read_prop 'spring\.datasource\.url')
SPRING_DATASOURCE_USERNAME=$(read_prop 'spring\.datasource\.username')
SPRING_DATASOURCE_PASSWORD=$(read_prop 'spring\.datasource\.password')
: "${SPRING_DATASOURCE_URL:?spring.datasource.url not set in $PROD_CONFIG_FILE}"
: "${SPRING_DATASOURCE_USERNAME:?spring.datasource.username not set in $PROD_CONFIG_FILE}"
: "${SPRING_DATASOURCE_PASSWORD:?spring.datasource.password not set in $PROD_CONFIG_FILE}"

# jdbc:mysql://host:port/dbname?params -> host, port, dbname
url=${SPRING_DATASOURCE_URL#jdbc:mysql://}
hostport=${url%%/*}
PROD_HOST=${hostport%%:*}
PROD_PORT=${hostport##*:}
rest=${url#*/}
PROD_DB=${rest%%\?*}

echo "==> Dumping production database '$PROD_DB' (read-only mysqldump) ..."
mysqldump --single-transaction --routines --triggers --no-tablespaces \
  -h "$PROD_HOST" -P "$PROD_PORT" -u "$SPRING_DATASOURCE_USERNAME" -p"$SPRING_DATASOURCE_PASSWORD" \
  "$PROD_DB" >"$DUMP_FILE"
chmod 600 "$DUMP_FILE"

echo "==> Starting a disposable MySQL container ..."
docker run -d --rm --name "$CONTAINER_NAME" \
  -e MYSQL_ROOT_PASSWORD="$ROOT_PASSWORD" \
  -e MYSQL_DATABASE="$PROD_DB" \
  -p 127.0.0.1::3306 \
  mysql:8.0 >/dev/null

REHEARSAL_PORT=$(docker port "$CONTAINER_NAME" 3306/tcp | head -1 | cut -d: -f2)

echo "==> Waiting for the disposable database to finish initializing ..."
# The official mysql image boots a temporary internal server (socket-only,
# "port: 0") to run init scripts and set the root password, then restarts
# into the final server on the real port. Both `mysqladmin ping` and a naive
# count of "ready for connections" lines (that phrase also appears in the
# unrelated "X Plugin ready for connections" line, so counting hits 2 during
# the temporary phase alone) can report ready before the final server with
# the real root password is actually up -- so wait specifically for the
# final server's own ready line, identified by its real port (3306 inside
# the container, regardless of the random host-side port mapping).
for i in $(seq 1 60); do
  if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "mysqld: ready for connections.*port: 3306"; then
    break
  fi
  [[ $i -eq 60 ]] && { echo "FAIL: disposable MySQL never finished initializing." >&2; exit 1; }
  sleep 2
done

echo "==> Loading the production dump into the disposable database ..."
docker exec -i "$CONTAINER_NAME" mysql -uroot -p"$ROOT_PASSWORD" "$PROD_DB" <"$DUMP_FILE"

echo "==> Building the application jar ..."
cd "$APP_DIR"
./mvnw -q clean package -DskipTests

JAR=$(find target -maxdepth 1 -name 'familytree-*.jar' ! -name '*.original' | head -1)
[[ -n "$JAR" ]] || { echo "FAIL: could not find built jar in target/." >&2; exit 1; }

echo "==> Starting the app against the disposable database to run Flyway for real ..."
# Mirror the real systemd ExecStart (--spring.config.additional-location)
# so app.admin.* etc. come from the real prod config -- AdminUserInitializer
# runs registerAdminIfMissing() on every startup, and it must see the same
# admin username the dumped data already has, or it'll try to INSERT a new
# admin row that doesn't exist in this rehearsal run (which surfaced a real,
# separate latent bug: app_users.id has no AUTO_INCREMENT/default, so that
# insert fails -- harmless in real prod only because the real admin username
# always already exists there). Env vars still win over the config file, so
# SPRING_DATASOURCE_* below correctly overrides to point at the disposable
# database. --server.port=0 binds an OS-assigned free port since the real
# prod app is already running on 8080 on this same host.
SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:${REHEARSAL_PORT}/${PROD_DB}" \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD="$ROOT_PASSWORD" \
  java -jar "$JAR" --spring.profiles.active=prod --server.port=0 \
  --spring.config.additional-location=file:/srv/config/familytree/ >"$APP_LOG" 2>&1 &
APP_PID=$!

echo "==> Waiting for startup / migration result ..."
for i in $(seq 1 60); do
  if grep -q "Started FamilyTreeApplication" "$APP_LOG" 2>/dev/null; then
    echo "PASS: migrations applied cleanly against a production-shaped copy of the database."
    exit 0
  fi
  if grep -qiE "FlywayException|APPLICATION FAILED TO START" "$APP_LOG" 2>/dev/null; then
    echo "FAIL: migration rehearsal failed. App log follows:" >&2
    cat "$APP_LOG" >&2
    exit 1
  fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "FAIL: app process exited unexpectedly. App log follows:" >&2
    cat "$APP_LOG" >&2
    exit 1
  fi
  sleep 2
done

echo "FAIL: timed out waiting for app startup. App log follows:" >&2
cat "$APP_LOG" >&2
exit 1
