package com.interview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WorkOrderPartRequest(
    @NotNull(message = "partId is required")
    UUID partId,

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    Integer quantity
) {
}
