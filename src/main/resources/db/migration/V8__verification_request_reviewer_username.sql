-- Admins currently authenticate via the old AppUser system (username
-- only, no UserAccount row) -- reviewed_by_user_account_id genuinely
-- doesn't apply to any admin yet, so this plain username column is the
-- only real record of who reviewed a request until the two systems are
-- unified.
ALTER TABLE verification_requests
    ADD COLUMN reviewed_by_username VARCHAR(255) NULL AFTER reviewed_by_user_account_id;
