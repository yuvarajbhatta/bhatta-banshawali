"use client";

import { useEffect, useState, type FormEvent } from "react";
import { motion } from "framer-motion";
import { Eye, EyeOff } from "lucide-react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/Button";
import { DURATION, EASE_OUT } from "@/lib/motion";
import styles from "./LoginForm.module.css";

function readXsrfTokenCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}

export function LoginForm() {
  const t = useTranslations("login");
  // Starts null, not a lazy `document.cookie` read -- this is a "use
  // client" component, but Next.js still server-renders it for the
  // initial HTML, and `document` doesn't exist during that SSR pass.
  const [csrfToken, setCsrfToken] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [passwordVisible, setPasswordVisible] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function ensureCsrfToken(): Promise<string | null> {
      const existing = readXsrfTokenCookie();
      if (existing) {
        return existing;
      }
      // A first-time visitor's browser has never talked to the backend
      // directly yet (this page itself is served by the Next.js app,
      // not the backend), so there's no XSRF-TOKEN cookie yet. Any GET
      // to the backend triggers it to write one (CsrfCookieFilter
      // forces this on every request) -- reusing the public stats
      // endpoint here since it's already a cheap, permitAll GET,
      // rather than adding a dedicated endpoint just for this.
      await fetch("/api/v1/public-stats").catch(() => null);
      return readXsrfTokenCookie();
    }

    ensureCsrfToken().then((token) => {
      if (!cancelled) {
        setCsrfToken(token);
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    if (!csrfToken) {
      // Extremely unlikely (the bootstrap fetch above resolves in
      // milliseconds, well before a human fills in credentials), but
      // fail closed rather than submitting a form with no CSRF value.
      event.preventDefault();
      return;
    }
    setSubmitting(true);
  }

  return (
    <motion.form
      className={styles.form}
      action="/api/v1/auth/login"
      method="post"
      onSubmit={handleSubmit}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: DURATION.slow, ease: EASE_OUT }}
    >
      <input type="hidden" name="_csrf" value={csrfToken ?? ""} />

      <div className={styles.field}>
        <label htmlFor="username">{t("username")}</label>
        <input id="username" name="username" type="text" autoComplete="username" required />
      </div>

      <div className={styles.field}>
        <label htmlFor="password">{t("password")}</label>
        <div className={styles.passwordWrapper}>
          <input
            id="password"
            name="password"
            type={passwordVisible ? "text" : "password"}
            autoComplete="current-password"
            required
          />
          <button
            type="button"
            className={styles.passwordToggle}
            onClick={() => setPasswordVisible((current) => !current)}
            aria-label={passwordVisible ? t("hidePassword") : t("showPassword")}
            aria-pressed={passwordVisible}
          >
            {passwordVisible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
          </button>
        </div>
        <Link href="/forgot-password" className={styles.forgotPasswordLink}>
          {t("forgotPassword")}
        </Link>
      </div>

      <Button type="submit" variant="primary" className={styles.submit} disabled={!csrfToken || submitting}>
        {!csrfToken ? t("preparing") : submitting ? t("submitting") : t("submit")}
      </Button>

      <p className={styles.bottom}>
        {t("bottom")} <Link href="/signup">{t("signUp")}</Link>
      </p>
    </motion.form>
  );
}
