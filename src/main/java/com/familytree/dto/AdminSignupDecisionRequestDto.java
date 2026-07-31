package com.familytree.dto;

import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/admin/signups/{id}/{approve|reject|request-more-info}.
 * linkedPersonId and createAsChildOfFatherId are only meaningful for approve
 * (docs/05: the admin's confirmed match from the candidates list) --
 * ignored by reject and request-more-info. Mutually exclusive: linkedPersonId
 * links the account to an existing Person; createAsChildOfFatherId creates a
 * brand-new Person as that father's child (see VerificationReviewService.approve).
 */
public class AdminSignupDecisionRequestDto {

    @Size(max = 2000, message = "Decision note must be 2000 characters or fewer.")
    private String decisionNote;

    private Long linkedPersonId;

    private Long createAsChildOfFatherId;

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
}
