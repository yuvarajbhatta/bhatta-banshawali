package com.familytree.dto;

/**
 * Consistent shape for API validation-error responses -- see
 * docs/18-api-design (consistent error model). Deliberately just a
 * message, no stack trace or internal details leaked to the client.
 */
public record ErrorResponseDto(String message) {
}
