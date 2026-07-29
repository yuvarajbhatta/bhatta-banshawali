package com.familytree.repository;

import com.familytree.entity.VerificationRequest;
import com.familytree.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    List<VerificationRequest> findAllByStatusOrderByCreatedAtAsc(VerificationStatus status);
    List<VerificationRequest> findByUserAccountId(Long userAccountId);

    /** Rows where this account acted as the reviewer, not the applicant -- see UserAccountAdminService#delete. */
    List<VerificationRequest> findByReviewedById(Long userAccountId);
}
