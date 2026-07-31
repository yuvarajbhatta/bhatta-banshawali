package com.familytree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A signup applicant's submitted identity/lineage information and the
 * resulting family-match outcome -- see docs/05-auth-and-verification.md.
 * Match evidence (matchedCandidatePersonIds) is admin-only by
 * convention: no controller ever returns this field to the applicant --
 * see SignupController/SignupResponseDto, which never reads this entity
 * at all.
 */
@Entity
@Table(name = "verification_requests")
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(nullable = false, length = 255)
    private String submittedFullName;

    @Column(length = 255)
    private String submittedFullNameNepali;

    @Column(nullable = false, length = 255)
    private String submittedFatherName;

    @Column(nullable = false, length = 255)
    private String submittedGrandfatherName;

    private LocalDate submittedDobAd;

    private Integer submittedDobBsYear;
    private Integer submittedDobBsMonth;
    private Integer submittedDobBsDay;

    // Optional, admin-configurable fields -- see docs/05 "Additional
    // Optional Verification Fields". None are required to submit.
    @Column(length = 255)
    private String motherName;

    @Column(length = 255)
    private String placeOfBirth;

    @Column(length = 255)
    private String ancestralVillage;

    @Column(length = 255)
    private String familyBranch;

    @Column(length = 255)
    private String knownRelativeName;

    @Column(length = 100)
    private String invitationCode;

    @Column(length = 2000)
    private String applicantNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchConfidence matchConfidence;

    /** Comma-separated Person IDs the matcher considered -- admin-only, never returned to the applicant. */
    @Column(length = 4000)
    private String matchedCandidatePersonIds;

    /**
     * Comma-separated Person IDs of candidate FATHERS found by searching on
     * the applicant's stated father's name (distinct from
     * matchedCandidatePersonIds, which searches on the applicant's OWN
     * name) -- admin-only, never returned to the applicant.
     */
    @Column(length = 4000)
    private String matchedFatherCandidatePersonIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_account_id")
    private UserAccount reviewedBy;

    /**
     * Who actually reviewed this, captured as a plain username. Admins
     * currently authenticate via the old AppUser system (username/password,
     * no UserAccount row at all) -- reviewedBy above is for once that's
     * unified, but right now no admin has a UserAccount to reference, so
     * this is the only genuine record of who acted.
     */
    @Column(length = 255)
    private String reviewedByUsername;

    private LocalDateTime reviewedAt;

    @Column(length = 2000)
    private String decisionNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public String getSubmittedFullName() {
        return submittedFullName;
    }

    public void setSubmittedFullName(String submittedFullName) {
        this.submittedFullName = submittedFullName;
    }

    public String getSubmittedFullNameNepali() {
        return submittedFullNameNepali;
    }

    public void setSubmittedFullNameNepali(String submittedFullNameNepali) {
        this.submittedFullNameNepali = submittedFullNameNepali;
    }

    public String getSubmittedFatherName() {
        return submittedFatherName;
    }

    public void setSubmittedFatherName(String submittedFatherName) {
        this.submittedFatherName = submittedFatherName;
    }

    public String getSubmittedGrandfatherName() {
        return submittedGrandfatherName;
    }

    public void setSubmittedGrandfatherName(String submittedGrandfatherName) {
        this.submittedGrandfatherName = submittedGrandfatherName;
    }

    public LocalDate getSubmittedDobAd() {
        return submittedDobAd;
    }

    public void setSubmittedDobAd(LocalDate submittedDobAd) {
        this.submittedDobAd = submittedDobAd;
    }

    public Integer getSubmittedDobBsYear() {
        return submittedDobBsYear;
    }

    public void setSubmittedDobBsYear(Integer submittedDobBsYear) {
        this.submittedDobBsYear = submittedDobBsYear;
    }

    public Integer getSubmittedDobBsMonth() {
        return submittedDobBsMonth;
    }

    public void setSubmittedDobBsMonth(Integer submittedDobBsMonth) {
        this.submittedDobBsMonth = submittedDobBsMonth;
    }

    public Integer getSubmittedDobBsDay() {
        return submittedDobBsDay;
    }

    public void setSubmittedDobBsDay(Integer submittedDobBsDay) {
        this.submittedDobBsDay = submittedDobBsDay;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getAncestralVillage() {
        return ancestralVillage;
    }

    public void setAncestralVillage(String ancestralVillage) {
        this.ancestralVillage = ancestralVillage;
    }

    public String getFamilyBranch() {
        return familyBranch;
    }

    public void setFamilyBranch(String familyBranch) {
        this.familyBranch = familyBranch;
    }

    public String getKnownRelativeName() {
        return knownRelativeName;
    }

    public void setKnownRelativeName(String knownRelativeName) {
        this.knownRelativeName = knownRelativeName;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getApplicantNote() {
        return applicantNote;
    }

    public void setApplicantNote(String applicantNote) {
        this.applicantNote = applicantNote;
    }

    public MatchConfidence getMatchConfidence() {
        return matchConfidence;
    }

    public void setMatchConfidence(MatchConfidence matchConfidence) {
        this.matchConfidence = matchConfidence;
    }

    public String getMatchedCandidatePersonIds() {
        return matchedCandidatePersonIds;
    }

    public void setMatchedCandidatePersonIds(String matchedCandidatePersonIds) {
        this.matchedCandidatePersonIds = matchedCandidatePersonIds;
    }

    public String getMatchedFatherCandidatePersonIds() {
        return matchedFatherCandidatePersonIds;
    }

    public void setMatchedFatherCandidatePersonIds(String matchedFatherCandidatePersonIds) {
        this.matchedFatherCandidatePersonIds = matchedFatherCandidatePersonIds;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public UserAccount getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UserAccount reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewedByUsername() {
        return reviewedByUsername;
    }

    public void setReviewedByUsername(String reviewedByUsername) {
        this.reviewedByUsername = reviewedByUsername;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
