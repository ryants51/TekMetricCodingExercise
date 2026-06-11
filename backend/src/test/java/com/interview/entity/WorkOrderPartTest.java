package com.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.dto.WorkOrderPartRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkOrderPartTest {
    @Test
    void fromCreatesWorkOrderPartFromResolvedEntitiesAndRequest() {
        WorkOrder workOrder = WorkOrder.builder().id(UUID.randomUUID()).build();
        Part part = part(new BigDecimal("12.50"));

        WorkOrderPart workOrderPart = WorkOrderPart.from(
            workOrder,
            part,
            new WorkOrderPartRequest(part.getId(), 4)
        );

        assertThat(workOrderPart.getWorkOrder()).isEqualTo(workOrder);
        assertThat(workOrderPart.getPart()).isEqualTo(part);
        assertThat(workOrderPart.getQuantity()).isEqualTo(4);
        assertThat(workOrderPart.calculateCost()).isEqualByComparingTo("50.00");
    }

    private Part part(BigDecimal price) {
        return Part.builder()
            .id(UUID.randomUUID())
            .sku(41002)
            .manufacturer("Denso")
            .name("Iridium Spark Plug")
            .price(price)
            .build();
    }
}
