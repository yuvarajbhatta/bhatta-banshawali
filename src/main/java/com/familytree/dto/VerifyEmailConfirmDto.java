package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailConfirmDto(@NotBlank String token) {
}
