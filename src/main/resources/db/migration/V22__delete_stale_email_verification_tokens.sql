-- TokenPurpose.EMAIL_VERIFICATION was retired in favor of OTP-based email
-- verification (see OtpService, V20) -- any pre-existing rows with that
-- purpose can no longer be deserialized into the enum, which broke admin
-- account deletion (UserAccountAdminService.delete loads every token row
-- for the account via findByUserAccountId, purpose-agnostic). Safe to
-- delete outright: they're all either already-consumed or now-expired
-- verification links nobody can act on anymore.
DELETE FROM user_account_tokens WHERE purpose = 'EMAIL_VERIFICATION';
