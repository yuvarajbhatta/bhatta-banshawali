-- News & Alerts: admin-authored broadcast posts (app updates, family
-- news, celebrations, obituaries, help requests), each optionally with
-- photos. No member submission path and no comments/reactions -- a
-- broadcast feed, not a discussion board. See AnnouncementPost/
-- AnnouncementPhoto and AnnouncementAdminService.
CREATE TABLE announcement_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(30) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    title_ne VARCHAR(255) NULL,
    body_en TEXT NOT NULL,
    body_ne TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME NULL,
    created_by_username VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE INDEX idx_announcement_posts_status_published_at ON announcement_posts (status, published_at);

-- storage_key is a server-generated filename (never derived from the
-- client's original filename); the bytes live on disk outside the web
-- root (see AnnouncementAdminService), same convention as person_photos.
CREATE TABLE announcement_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_post_id BIGINT NOT NULL,
    uploaded_by_username VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    caption VARCHAR(500) NULL,
    uploaded_at DATETIME NOT NULL,
    CONSTRAINT fk_announcement_photos_post FOREIGN KEY (announcement_post_id) REFERENCES announcement_posts (id)
);

CREATE INDEX idx_announcement_photos_post_id ON announcement_photos (announcement_post_id);

-- Unread-badge watermark (see UserAccount.lastSeenAnnouncementsAt) --
-- null for every existing account, meaning every published post counts
-- as unread the first time each member opens the News tab.
ALTER TABLE user_accounts
    ADD COLUMN last_seen_announcements_at DATETIME NULL;
