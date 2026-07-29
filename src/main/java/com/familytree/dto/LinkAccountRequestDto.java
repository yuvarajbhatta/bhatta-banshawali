package com.familytree.dto;

import jakarta.validation.constraints.NotNull;

public class LinkAccountRequestDto {

    @NotNull(message = "Person is required.")
    private Long personId;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }
}
