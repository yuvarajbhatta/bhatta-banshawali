package com.familytree.dto;

import jakarta.validation.constraints.NotNull;

public class MergeRequestDto {

    @NotNull(message = "Survivor person is required.")
    private Long survivorId;

    @NotNull(message = "Person to merge away is required.")
    private Long loserId;

    public Long getSurvivorId() {
        return survivorId;
    }

    public void setSurvivorId(Long survivorId) {
        this.survivorId = survivorId;
    }

    public Long getLoserId() {
        return loserId;
    }

    public void setLoserId(Long loserId) {
        this.loserId = loserId;
    }
}
