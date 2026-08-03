-- Picture Album (docs/06-ui-ux-specification.md dashboard) -- a
-- multi-photo gallery per Person. storage_key is a server-generated
-- filename (never derived from the client's original filename, closes
-- path traversal); the actual bytes live on disk outside the web root
-- (see PersonPhotoService), not in this table.
CREATE TABLE person_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    uploaded_by_user_account_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    caption VARCHAR(500) NULL,
    uploaded_at DATETIME NOT NULL,
    CONSTRAINT fk_person_photos_person FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT fk_person_photos_uploader FOREIGN KEY (uploaded_by_user_account_id) REFERENCES user_accounts (id)
);

CREATE INDEX idx_person_photos_person_id ON person_photos (person_id);
