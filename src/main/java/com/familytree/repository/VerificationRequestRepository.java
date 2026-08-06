package com.familytree.repository;

import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    List<VerificationRequest> findAllByStatusOrderByCreatedAtAsc(VerificationStatus status);
    List<VerificationRequest> findByUserAccountId(Long userAccountId);

    /** Rows where this account acted as the reviewer, not the applicant -- see UserAccountAdminService#delete. */
    List<VerificationRequest> findByReviewedById(Long userAccountId);

    /** See SignupService.uploadPendingPhoto -- the still-unauthenticated applicant's own handle for their pending request. */
    Optional<VerificationRequest> findByPhotoUploadToken(String photoUploadToken);
}
