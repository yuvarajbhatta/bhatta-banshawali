package com.familytree.repository;

import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.PersonCorrectionRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonCorrectionRequestRepository extends JpaRepository<PersonCorrectionRequest, Long> {
    /**
     * Eagerly fetches person/submittedBy -- admin-corrections.html reads
     * both (request.person.id, request.submittedBy.email) while rendering,
     * and with spring.jpa.open-in-view=false the transaction this query
     * runs in is long closed by render time, so a plain lazy load here
     * would throw LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"person", "submittedBy"})
    List<PersonCorrectionRequest> findAllByStatusOrderBySubmittedAtAsc(CorrectionRequestStatus status);

    /** Corrections this account submitted -- see UserAccountAdminService#delete. */
    List<PersonCorrectionRequest> findBySubmittedById(Long userAccountId);

    /** Pending-corrections-mine dashboard stat -- see MemberProfileController. */
    long countBySubmittedByIdAndStatus(Long userAccountId, CorrectionRequestStatus status);

    /** Corrections about this person -- see PersonMergeService#merge. */
    List<PersonCorrectionRequest> findByPersonId(Long personId);
}
