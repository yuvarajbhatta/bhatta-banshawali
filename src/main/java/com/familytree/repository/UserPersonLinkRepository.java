package com.familytree.repository;

import com.familytree.entity.UserPersonLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPersonLinkRepository extends JpaRepository<UserPersonLink, Long> {
    List<UserPersonLink> findByPersonId(Long personId);

    /**
     * Eagerly fetches person -- most callers (MemberProfileController,
     * ViewerContextResolver, PersonPhotoService, AdminAccessRequestService,
     * UserAccountAdminService) immediately map results to
     * UserPersonLink::getPerson, and with spring.jpa.open-in-view=false the
     * transaction this query runs in is long closed by the time they do
     * that -- a plain lazy load throws LazyInitializationException
     * (surfaced live as GET /api/v1/me -> 500, "no session" on the Person
     * proxy).
     */
    @EntityGraph(attributePaths = {"person"})
    List<UserPersonLink> findByUserAccountId(Long userAccountId);

    /** Rows where this account acted as the verifying admin -- see UserAccountAdminService#delete. */
    List<UserPersonLink> findByVerifiedById(Long userAccountId);
}
