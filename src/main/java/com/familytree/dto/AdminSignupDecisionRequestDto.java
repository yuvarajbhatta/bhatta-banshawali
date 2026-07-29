package com.familytree.dto;

import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/admin/signups/{id}/{approve|reject|request-more-info}.
 * linkedPersonId is only meaningful for approve (docs/05: the admin's
 * confirmed match from the candidates list) -- ignored by reject and
 * request-more-info.
 */
public class AdminSignupDecisionRequestDto {

    @Size(max = 2000, message = "Decision note must be 2000 characters or fewer.")
    private String decisionNote;

    private Long linkedPersonId;

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
}
