package com.interview.dto;

import com.interview.entity.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkOrderResponse(
    UUID id,
    UUID vehicleId,
    WorkOrderStatus status,
    String summary,
    String notes,
    BigDecimal laborRate,
    BigDecimal laborTime,
    BigDecimal laborCost,
    BigDecimal totalCost,
    String estimateUrl,
    Instant createdAt,
    Instant updatedAt,
    List<WorkOrderPartResponse> partsNeeded
) {
}
