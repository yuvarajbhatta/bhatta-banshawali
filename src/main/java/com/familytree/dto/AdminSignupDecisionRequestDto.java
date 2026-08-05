package com.familytree.dto;

import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/admin/signups/{id}/{approve|reject|request-more-info}.
 * linkedPersonId, createAsChildOfFatherId, and linkMatchedMother are only
 * meaningful for approve (docs/05: the admin's confirmed match from the
 * candidates list) -- ignored by reject and request-more-info.
 * linkedPersonId and createAsChildOfFatherId are mutually exclusive:
 * linkedPersonId links the account to an existing Person;
 * createAsChildOfFatherId creates a brand-new Person as that father's
 * child (see VerificationReviewService.approve). linkMatchedMother only
 * has any effect alongside createAsChildOfFatherId, when that father has
 * a recorded spouse matching the applicant's submitted mother's name
 * (FatherCandidateDto.matchedMother) -- true (the default the frontend
 * sends when its confirmation checkbox is checked) also links her as the
 * new Person's mother in the same approval.
 */
public class AdminSignupDecisionRequestDto {

    @Size(max = 2000, message = "Decision note must be 2000 characters or fewer.")
    private String decisionNote;

    private Long linkedPersonId;

    private Long createAsChildOfFatherId;

    private Boolean linkMatchedMother;

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public Long getLinkedPersonId() {
        return linkedPersonId;
    }

    public void setLinkedPersonId(Long linkedPersonId) {
        this.linkedPersonId = linkedPersonId;
    }

    public Long getCreateAsChildOfFatherId() {
        return createAsChildOfFatherId;
    }

    public void setCreateAsChildOfFatherId(Long createAsChildOfFatherId) {
        this.createAsChildOfFatherId = createAsChildOfFatherId;
    }

    public Boolean getLinkMatchedMother() {
        return linkMatchedMother;
    }

    public void setLinkMatchedMother(Boolean linkMatchedMother) {
        this.linkMatchedMother = linkMatchedMother;
    }
}
