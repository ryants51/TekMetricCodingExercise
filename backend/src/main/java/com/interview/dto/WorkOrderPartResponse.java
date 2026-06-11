package com.interview.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkOrderPartResponse(
    UUID partId,
    Integer sku,
    String manufacturer,
    String name,
    BigDecimal price,
    Integer quantity,
    BigDecimal totalCost
) {
}
