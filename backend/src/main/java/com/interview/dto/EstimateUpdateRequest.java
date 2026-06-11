package com.interview.dto;

import com.interview.entity.EstimateStatus;
import jakarta.validation.constraints.NotNull;

public record EstimateUpdateRequest(
    @NotNull(message = "status is required")
    EstimateStatus status
) {
}
