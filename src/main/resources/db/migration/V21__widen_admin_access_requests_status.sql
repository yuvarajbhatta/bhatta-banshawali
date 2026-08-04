-- AWAITING_OTP_CONFIRMATION (25 chars) no longer fits VARCHAR(20) -- see
-- com.familytree.entity.AdminAccessRequestStatus.
ALTER TABLE admin_access_requests MODIFY COLUMN status VARCHAR(30) NOT NULL;
