-- Lets a signup applicant attach a profile photo before any Person
-- record exists for them (that only happens on admin approval, see
-- VerificationReviewService) -- photo_upload_token is a random,
-- unguessable handle returned once from POST /api/v1/signup so a
-- separate, still-unauthenticated follow-up request (POST
-- /api/v1/signup/photo) can attach a photo to the right pending
-- request without a real session. pending_photo_storage_key mirrors
-- person_photos.storage_key (see V17): a server-generated filename,
-- never derived from the client's original one.
ALTER TABLE verification_requests
    ADD COLUMN photo_upload_token VARCHAR(36) NULL,
    ADD COLUMN pending_photo_storage_key VARCHAR(255) NULL;

CREATE UNIQUE INDEX idx_verification_requests_photo_upload_token ON verification_requests (photo_upload_token);
