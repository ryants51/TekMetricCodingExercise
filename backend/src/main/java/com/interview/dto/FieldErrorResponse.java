package com.interview.dto;

public record FieldErrorResponse(
    String field,
    String message
) {
}
