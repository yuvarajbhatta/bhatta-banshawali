-- Mirrors user_accounts.last_seen_announcements_at (V18) for the legacy
-- app_users login table -- today's only admin accounts are AppUser rows,
-- not UserAccount, so without this the News & Alerts unread badge could
-- never track a watermark for them (see AnnouncementService).
ALTER TABLE app_users
    ADD COLUMN last_seen_announcements_at DATETIME NULL;
