package com.familytree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Links a {@link UserAccount} to the {@link Person} record it has been
 * verified as belonging to. A user does not gain access to a person record
 * merely by claiming the same name -- linkStatus starts PENDING and only
 * becomes VERIFIED through the admin-reviewed verification workflow
 * (see docs/05-auth-and-verification.md).
 *
 * Deliberately not database-unique on person_id: "at most one VERIFIED link
 * per person" is a conditional constraint (it does not apply to PENDING or
 * REJECTED rows), which plain unique constraints cannot express portably
 * across MySQL and H2. That invariant is enforced in the verification
 * service once it exists (Phase 3), not here.
 */
@Entity
@Table(name = "user_person_links")
public class UserPersonLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id")
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserPersonLinkStatus linkStatus = UserPersonLinkStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_account_id")
    private UserAccount verifiedBy;

    private LocalDateTime verifiedAt;

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public UserPersonLinkStatus getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(UserPersonLinkStatus linkStatus) {
        this.linkStatus = linkStatus;
    }

    public UserAccount getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UserAccount verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
