package com.interview.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EstimateRequest(
    // If we had a customer table, we would want to check that the customerId actually exists
    @NotNull(message = "customerId is required")
    UUID customerId,

    // If we had a vehicles table, we would want to check that the vehicleId actually exists
    @NotNull(message = "vehicleId is required")
    UUID vehicleId
) {
}
