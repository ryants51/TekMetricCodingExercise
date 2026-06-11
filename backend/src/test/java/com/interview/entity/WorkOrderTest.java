package com.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.dto.WorkOrderPartRequest;
import com.interview.dto.WorkOrderRequest;
import com.interview.dto.WorkOrderResponse;
import com.interview.dto.WorkOrderUpdateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkOrderTest {
    @Test
    void fromCreatesWorkOrderFromCreateRequest() {
        UUID vehicleId = UUID.randomUUID();

        WorkOrder workOrder = WorkOrder.from(new WorkOrderRequest(
            vehicleId,
            WorkOrderStatus.PENDING,
            "Replace spark plugs",
            "Customer approved premium plugs.",
            new BigDecimal("100.00"),
            new BigDecimal("2.50"),
            List.of(new WorkOrderPartRequest(UUID.randomUUID(), 4))
        ));

        assertThat(workOrder.getVehicleId()).isEqualTo(vehicleId);
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.PENDING);
        assertThat(workOrder.getSummary()).isEqualTo("Replace spark plugs");
        assertThat(workOrder.getNotes()).isEqualTo("Customer approved premium plugs.");
        assertThat(workOrder.getLaborRate()).isEqualByComparingTo("100.00");
        assertThat(workOrder.getLaborTime()).isEqualByComparingTo("2.50");
        assertThat(workOrder.getPartsNeeded()).isEmpty();
    }

    @Test
    void updateFromChangesEditableFieldsWithoutChangingVehicleId() {
        UUID vehicleId = UUID.randomUUID();
        WorkOrder workOrder = WorkOrder.builder()
            .vehicleId(vehicleId)
            .status(WorkOrderStatus.PENDING)
            .summary("Old summary")
            .notes("Old notes")
            .laborRate(new BigDecimal("90.00"))
            .laborTime(new BigDecimal("1.00"))
            .build();

        workOrder.updateFrom(new WorkOrderUpdateRequest(
            WorkOrderStatus.ACCEPTED,
            "Replace ignition components",
            "Updated after technician inspection.",
            new BigDecimal("120.00"),
            new BigDecimal("1.50"),
            List.of(new WorkOrderPartRequest(UUID.randomUUID(), 2))
        ));

        assertThat(workOrder.getVehicleId()).isEqualTo(vehicleId);
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.ACCEPTED);
        assertThat(workOrder.getSummary()).isEqualTo("Replace ignition components");
        assertThat(workOrder.getNotes()).isEqualTo("Updated after technician inspection.");
        assertThat(workOrder.getLaborRate()).isEqualByComparingTo("120.00");
        assertThat(workOrder.getLaborTime()).isEqualByComparingTo("1.50");
    }

    @Test
    void replacePartsNeededClearsExistingPartsAndAssignsParent() {
        WorkOrder workOrder = WorkOrder.builder()
            .vehicleId(UUID.randomUUID())
            .status(WorkOrderStatus.PENDING)
            .summary("Replace spark plugs")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("1.00"))
            .build();
        WorkOrderPart existingPart = WorkOrderPart.builder()
            .workOrder(workOrder)
            .part(part(new BigDecimal("10.00")))
            .quantity(1)
            .build();
        WorkOrderPart replacementPart = WorkOrderPart.builder()
            .part(part(new BigDecimal("12.50")))
            .quantity(4)
            .build();
        workOrder.getPartsNeeded().add(existingPart);

        workOrder.replacePartsNeeded(List.of(replacementPart));

        assertThat(workOrder.getPartsNeeded()).containsExactly(replacementPart);
        assertThat(replacementPart.getWorkOrder()).isEqualTo(workOrder);
    }

    @Test
    void replacePartsNeededUpdatesExistingPartQuantityInPlace() {
        WorkOrder workOrder = WorkOrder.builder()
            .vehicleId(UUID.randomUUID())
            .status(WorkOrderStatus.PENDING)
            .summary("Replace spark plugs")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("1.00"))
            .build();
        Part part = part(new BigDecimal("12.50"));
        WorkOrderPart existingPart = WorkOrderPart.builder()
            .id(UUID.randomUUID())
            .workOrder(workOrder)
            .part(part)
            .quantity(1)
            .build();
        WorkOrderPart replacementPart = WorkOrderPart.builder()
            .part(part)
            .quantity(4)
            .build();
        workOrder.getPartsNeeded().add(existingPart);

        workOrder.replacePartsNeeded(List.of(replacementPart));

        assertThat(workOrder.getPartsNeeded()).containsExactly(existingPart);
        assertThat(existingPart.getQuantity()).isEqualTo(4);
    }

    @Test
    void toResponseIncludesEstimateUrlWhenAssociated() {
        Estimate estimate = Estimate.builder()
            .id(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .build();
        WorkOrder workOrder = workOrder(estimate.getVehicleId());
        estimate.addWorkOrder(workOrder);

        WorkOrderResponse response = workOrder.toResponse();

        assertThat(response.estimateUrl()).isEqualTo("/api/estimates/" + estimate.getId());
    }

    @Test
    void copyForEstimateClonesScalarsAndPartsForTargetEstimate() {
        Estimate targetEstimate = Estimate.builder()
            .id(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .build();
        WorkOrder source = workOrder(targetEstimate.getVehicleId());
        source.setId(UUID.randomUUID());

        WorkOrder clone = source.copyForEstimate(targetEstimate);

        assertThat(clone.getId()).isNull();
        assertThat(clone.getVehicleId()).isEqualTo(source.getVehicleId());
        assertThat(clone.getSummary()).isEqualTo(source.getSummary());
        assertThat(clone.getPartsNeeded()).hasSize(1);
        assertThat(clone.getPartsNeeded().getFirst().getId()).isNull();
        assertThat(clone.getPartsNeeded().getFirst().getPart()).isEqualTo(source.getPartsNeeded().getFirst().getPart());
        assertThat(clone.getPartsNeeded().getFirst().getQuantity()).isEqualTo(source.getPartsNeeded().getFirst().getQuantity());
        assertThat(clone.getEstimate()).isEqualTo(targetEstimate);
        assertThat(targetEstimate.getWorkOrders()).contains(clone);
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

    private WorkOrder workOrder(UUID vehicleId) {
        WorkOrder workOrder = WorkOrder.builder()
            .vehicleId(vehicleId)
            .status(WorkOrderStatus.PENDING)
            .summary("Replace spark plugs")
            .notes("Customer approved premium plugs.")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("1.00"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        WorkOrderPart workOrderPart = WorkOrderPart.builder()
            .part(part(new BigDecimal("12.50")))
            .quantity(4)
            .build();
        workOrder.replacePartsNeeded(List.of(workOrderPart));
        return workOrder;
    }
}
