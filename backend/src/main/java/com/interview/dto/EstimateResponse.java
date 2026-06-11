package com.interview.dto;

import com.interview.entity.EstimateStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EstimateResponse(
    UUID id,
    UUID customerId,
    UUID vehicleId,
    EstimateStatus status,
    BigDecimal totalCost,
    BigDecimal totalTime,
    Instant createdAt,
    Instant updatedAt,
    List<WorkOrderSummaryResponse> workOrders
) {
}
