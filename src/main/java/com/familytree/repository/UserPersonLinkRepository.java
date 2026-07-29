package com.familytree.repository;

import com.familytree.entity.UserPersonLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPersonLinkRepository extends JpaRepository<UserPersonLink, Long> {
    List<UserPersonLink> findByPersonId(Long personId);
    List<UserPersonLink> findByUserAccountId(Long userAccountId);

    /** Rows where this account acted as the verifying admin -- see UserAccountAdminService#delete. */
    List<UserPersonLink> findByVerifiedById(Long userAccountId);
}
