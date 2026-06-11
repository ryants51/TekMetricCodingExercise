package com.interview.dto;

import java.time.Instant;

public record HealthResponse(
    String status,
    Instant checkedAt
) {
}
