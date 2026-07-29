package com.familytree.dto;

import jakarta.validation.constraints.Size;

/** Body for POST /api/v1/admin/corrections/{id}/{approve|reject}. */
public class AdminDecisionRequestDto {

    @Size(max = 2000, message = "Decision note must be 2000 characters or fewer.")
    private String decisionNote;

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}
