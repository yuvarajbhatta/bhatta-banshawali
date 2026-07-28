package com.familytree.dto;

public record CorrectionResponseDto(String status) {

    public static CorrectionResponseDto submitted() {
        return new CorrectionResponseDto("SUBMITTED");
    }
}
