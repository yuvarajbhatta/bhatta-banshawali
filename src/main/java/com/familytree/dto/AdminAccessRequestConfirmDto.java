package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminAccessRequestConfirmDto(@NotBlank String code) {
}
