package com.familytree.repository;

import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.PersonCorrectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonCorrectionRequestRepository extends JpaRepository<PersonCorrectionRequest, Long> {
    List<PersonCorrectionRequest> findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus status);

    /** Corrections this account submitted -- see UserAccountAdminService#delete. */
    List<PersonCorrectionRequest> findBySubmittedById(Long userAccountId);

    /** Corrections about this person -- see PersonMergeService#merge. */
    List<PersonCorrectionRequest> findByPersonId(Long personId);
}
