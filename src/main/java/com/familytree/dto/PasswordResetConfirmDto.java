package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetConfirmDto(@NotBlank String token, @NotBlank String newPassword,
                                       @NotBlank String confirmNewPassword) {

    public boolean passwordsMatch() {
        return newPassword.equals(confirmNewPassword);
    }
}
