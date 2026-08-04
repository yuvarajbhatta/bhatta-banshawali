package com.familytree.repository;

import com.familytree.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Used by BridgingUserDetailsService: roles must be loaded eagerly here
     * since authentication reads them after the request's Hibernate session
     * has otherwise finished with this entity.
     */
    @Query("select ua from UserAccount ua left join fetch ua.roles where ua.email = :email")
    Optional<UserAccount> findByEmailWithRoles(String email);

    /** Used by AdminContactService -- the Help & Contact page's "who to reach" section. */
    @Query("select distinct ua from UserAccount ua join ua.roles r where r.name in :roleNames")
    List<UserAccount> findByRoles_NameIn(List<String> roleNames);
}
