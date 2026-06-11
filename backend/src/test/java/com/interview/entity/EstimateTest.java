package com.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.dto.EstimateRequest;
import com.interview.dto.EstimateResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstimateTest {
    @Test
    void fromCreatesPendingEstimateFromRequest() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Estimate estimate = Estimate.from(new EstimateRequest(customerId, vehicleId));

        assertThat(estimate.getCustomerId()).isEqualTo(customerId);
        assertThat(estimate.getVehicleId()).isEqualTo(vehicleId);
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.PENDING);
        assertThat(estimate.getWorkOrders()).isEmpty();
    }

    @Test
    void derivedTotalsExcludeRefusedWorkOrders() {
        WorkOrder pendingWorkOrder = workOrder(
            WorkOrderStatus.PENDING,
            new BigDecimal("125.00"),
            new BigDecimal("1.50"),
            new BigDecimal("40.00"),
            2,
            Instant.now()
        );
        WorkOrder acceptedWorkOrder = workOrder(
            WorkOrderStatus.ACCEPTED,
            new BigDecimal("150.00"),
            new BigDecimal("2.00"),
            new BigDecimal("30.00"),
            1,
            Instant.now()
        );
        WorkOrder refusedWorkOrder = workOrder(
            WorkOrderStatus.REFUSED,
            new BigDecimal("200.00"),
            new BigDecimal("3.00"),
            new BigDecimal("500.00"),
            1,
            Instant.now()
        );
        Estimate estimate = estimate(List.of(pendingWorkOrder, acceptedWorkOrder, refusedWorkOrder));

        assertThat(estimate.getTotalTime()).isEqualByComparingTo("3.50");
        assertThat(estimate.getTotalCost()).isEqualByComparingTo("597.50");
    }

    @Test
    void containsWorkOrderReturnsWhetherWorkOrderIsAssociated() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrder workOrder = workOrder(workOrderId, WorkOrderStatus.PENDING, Instant.now());
        Estimate estimate = estimate(List.of(workOrder));

        assertThat(estimate.containsWorkOrder(workOrderId)).isTrue();
        assertThat(estimate.containsWorkOrder(UUID.randomUUID())).isFalse();
    }

    @Test
    void toResponseMapsEstimateFieldsDerivedTotalsAndWorkOrderSummaries() {
        UUID estimateId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T11:00:00Z");
        WorkOrder workOrder = workOrder(
            WorkOrderStatus.ACCEPTED,
            new BigDecimal("110.00"),
            new BigDecimal("2.25"),
            new BigDecimal("25.00"),
            2,
            Instant.parse("2026-01-01T12:00:00Z")
        );
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(customerId)
            .vehicleId(vehicleId)
            .status(EstimateStatus.APPROVED)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .workOrders(List.of(workOrder))
            .build();

        EstimateResponse response = estimate.toResponse();

        assertThat(response.id()).isEqualTo(estimateId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        assertThat(response.status()).isEqualTo(EstimateStatus.APPROVED);
        assertThat(response.totalTime()).isEqualByComparingTo("2.25");
        assertThat(response.totalCost()).isEqualByComparingTo("297.50");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.workOrders()).hasSize(1);
        assertThat(response.workOrders().getFirst().id()).isEqualTo(workOrder.getId());
        assertThat(response.workOrders().getFirst().totalCost()).isEqualByComparingTo("297.5000");
    }

    @Test
    void toResponseAcceptsSpecificWorkOrdersForResponseCalculation() {
        WorkOrder associatedWorkOrder = workOrder(
            WorkOrderStatus.ACCEPTED,
            new BigDecimal("100.00"),
            new BigDecimal("5.00"),
            new BigDecimal("10.00"),
            1,
            Instant.now()
        );
        WorkOrder responseWorkOrder = workOrder(
            WorkOrderStatus.ACCEPTED,
            new BigDecimal("90.00"),
            new BigDecimal("1.00"),
            new BigDecimal("15.00"),
            2,
            Instant.now()
        );
        Estimate estimate = estimate(List.of(associatedWorkOrder));

        EstimateResponse response = estimate.toResponse(List.of(responseWorkOrder));

        assertThat(response.totalTime()).isEqualByComparingTo("1.00");
        assertThat(response.totalCost()).isEqualByComparingTo("120.00");
        assertThat(response.workOrders()).extracting("id").containsExactly(responseWorkOrder.getId());
    }

    @Test
    void toResponseSortsRefusedWorkOrdersAtTheBottomThenByCreatedAt() {
        WorkOrder newestAccepted = workOrder(
            UUID.randomUUID(),
            WorkOrderStatus.ACCEPTED,
            Instant.parse("2026-01-02T09:00:00Z")
        );
        WorkOrder oldestAccepted = workOrder(
            UUID.randomUUID(),
            WorkOrderStatus.PENDING,
            Instant.parse("2026-01-01T09:00:00Z")
        );
        WorkOrder refused = workOrder(
            UUID.randomUUID(),
            WorkOrderStatus.REFUSED,
            Instant.parse("2025-12-31T09:00:00Z")
        );
        Estimate estimate = estimate(List.of(newestAccepted, refused, oldestAccepted));

        EstimateResponse response = estimate.toResponse();

        assertThat(response.workOrders()).extracting("id")
            .containsExactly(oldestAccepted.getId(), newestAccepted.getId(), refused.getId());
    }

    private Estimate estimate(List<WorkOrder> workOrders) {
        return Estimate.builder()
            .id(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .status(EstimateStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .workOrders(workOrders)
            .build();
    }

    private WorkOrder workOrder(UUID id, WorkOrderStatus status, Instant createdAt) {
        WorkOrder workOrder = workOrder(
            status,
            new BigDecimal("100.00"),
            new BigDecimal("1.00"),
            new BigDecimal("25.00"),
            1,
            createdAt
        );
        workOrder.setId(id);
        return workOrder;
    }

    private WorkOrder workOrder(
        WorkOrderStatus status,
        BigDecimal laborRate,
        BigDecimal laborTime,
        BigDecimal partPrice,
        int partQuantity,
        Instant createdAt
    ) {
        WorkOrder workOrder = WorkOrder.builder()
            .id(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .status(status)
            .summary("Replace spark plugs")
            .notes("Customer approved premium plugs.")
            .laborRate(laborRate)
            .laborTime(laborTime)
            .createdAt(createdAt)
            .updatedAt(createdAt.plusSeconds(3600))
            .build();
        WorkOrderPart workOrderPart = WorkOrderPart.builder()
            .id(UUID.randomUUID())
            .workOrder(workOrder)
            .part(part(partPrice))
            .quantity(partQuantity)
            .build();
        workOrder.setPartsNeeded(List.of(workOrderPart));
        return workOrder;
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
