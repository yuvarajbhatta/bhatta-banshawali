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

import java.time.LocalDateTime;

/**
 * A member-submitted "this field about this person is wrong" request
 * (docs/08 Phase 4). Deliberately narrower than the full ChangeRequest
 * described in docs/04-data-model.md's target schema -- one Person
 * field per request, no before/after JSON snapshots of the whole
 * entity, no rollback, no non-Person change types (relationship edits,
 * merges, deletes). currentValueSnapshot is captured at submission time
 * for the admin's reference; approval overwrites whatever the field's
 * live value is at review time with proposedValue, it does not verify
 * the live value still matches the snapshot.
 */
@Entity
@Table(name = "person_correction_requests")
public class PersonCorrectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_account_id", nullable = false)
    private UserAccount submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CorrectablePersonField field;

    @Column(length = 1000)
    private String currentValueSnapshot;

    @Column(nullable = false, length = 1000)
    private String proposedValue;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorrectionRequestStatus status = CorrectionRequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    // Plain username, not a UserAccount reference -- admins currently
    // authenticate via the old AppUser system, same reasoning as
    // VerificationRequest.reviewedByUsername.
    @Column(length = 255)
    private String reviewedByUsername;

    private LocalDateTime reviewedAt;

    @Column(length = 2000)
    private String decisionNote;

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public UserAccount getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UserAccount submittedBy) {
        this.submittedBy = submittedBy;
    }

    public CorrectablePersonField getField() {
        return field;
    }

    public void setField(CorrectablePersonField field) {
        this.field = field;
    }

    public String getCurrentValueSnapshot() {
        return currentValueSnapshot;
    }

    public void setCurrentValueSnapshot(String currentValueSnapshot) {
        this.currentValueSnapshot = currentValueSnapshot;
    }

    public String getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(String proposedValue) {
        this.proposedValue = proposedValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CorrectionRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CorrectionRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
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
}
