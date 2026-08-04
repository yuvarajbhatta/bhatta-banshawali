package com.familytree.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailResendDto(@NotBlank @Email String email) {
}
