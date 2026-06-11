package com.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.dto.EstimateRequest;
import com.interview.dto.EstimateResponse;
import com.interview.dto.EstimateUpdateRequest;
import com.interview.dto.WorkOrderPartRequest;
import com.interview.dto.WorkOrderRequest;
import com.interview.entity.Estimate;
import com.interview.entity.EstimateStatus;
import com.interview.entity.Part;
import com.interview.entity.WorkOrder;
import com.interview.entity.WorkOrderPart;
import com.interview.entity.WorkOrderStatus;
import com.interview.exception.ConflictException;
import com.interview.repository.EstimateRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {
    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private EstimateService estimateService;

    @Test
    void createAllowsAnEstimateWithoutWorkOrders() {
        EstimateRequest request = new EstimateRequest(
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        when(estimateRepository.saveAndFlush(any(Estimate.class))).thenAnswer(invocation -> {
            Estimate estimate = invocation.getArgument(0);
            estimate.setId(UUID.randomUUID());
            estimate.setCreatedAt(Instant.now());
            estimate.setUpdatedAt(Instant.now());
            return estimate;
        });

        EstimateResponse response = estimateService.create(request);

        assertThat(response.workOrders()).isEmpty();
        assertThat(response.status()).isEqualTo(EstimateStatus.PENDING);
        assertThat(response.totalTime()).isEqualByComparingTo("0");
        assertThat(response.totalCost()).isEqualByComparingTo("0");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void getPlacesRefusedWorkOrdersAtTheBottom() {
        UUID estimateId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        WorkOrder refusedWorkOrder = workOrder(vehicleId, WorkOrderStatus.REFUSED);
        WorkOrder acceptedWorkOrder = workOrder(vehicleId, WorkOrderStatus.ACCEPTED);
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .workOrders(List.of(refusedWorkOrder, acceptedWorkOrder))
            .build();

        when(estimateRepository.findByIdWithWorkOrders(estimateId)).thenReturn(estimate);
        when(workOrderService.findAllWithPartsAndEstimateIncluded(List.of(refusedWorkOrder.getId(), acceptedWorkOrder.getId())))
            .thenReturn(List.of(refusedWorkOrder, acceptedWorkOrder));

        EstimateResponse response = estimateService.get(estimateId);

        assertThat(response.workOrders()).extracting("status")
            .containsExactly(WorkOrderStatus.ACCEPTED, WorkOrderStatus.REFUSED);
    }

    @Test
    void updateChangesStatusWithoutChangingWorkOrders() {
        UUID estimateId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        WorkOrder existingWorkOrder = workOrder(vehicleId, WorkOrderStatus.ACCEPTED);
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .workOrders(new ArrayList<>(List.of(existingWorkOrder)))
            .build();

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));

        EstimateResponse response = estimateService.update(
            estimateId,
            new EstimateUpdateRequest(EstimateStatus.APPROVED)
        );

        assertThat(response.status()).isEqualTo(EstimateStatus.APPROVED);
        assertThat(response.workOrders()).hasSize(1);
        assertThat(response.workOrders().getFirst().id()).isEqualTo(existingWorkOrder.getId());
        verify(estimateRepository, never()).save(estimate);
    }

    @Test
    void addWorkOrderCreatesAndAssociatesWorkOrderToEstimate() {
        UUID estimateId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        WorkOrder workOrder = workOrder(vehicleId, WorkOrderStatus.PENDING);
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .build();

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));
        when(workOrderService.createWorkOrderFromRequest(any(WorkOrderRequest.class))).thenReturn(workOrder);

        EstimateResponse response = estimateService.addWorkOrder(
            estimateId,
            new WorkOrderRequest(
                vehicleId,
                WorkOrderStatus.PENDING,
                "Replace spark plugs",
                "Customer approved premium plugs.",
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                List.of(new WorkOrderPartRequest(partId, 1))
            )
        );

        assertThat(response.workOrders()).hasSize(1);
        assertThat(response.workOrders().getFirst().id()).isEqualTo(workOrder.getId());
        verify(estimateRepository, never()).save(estimate);
    }

    @Test
    void addExistingWorkOrderAssociatesOneWorkOrderToEstimate() {
        UUID estimateId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .build();
        WorkOrder workOrder = workOrder(workOrderId, vehicleId, WorkOrderStatus.PENDING);

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));
        when(workOrderService.findEntityWithResponseGraph(workOrderId)).thenReturn(workOrder);

        EstimateResponse response = estimateService.addExistingWorkOrder(estimateId, workOrderId);

        assertThat(response.workOrders()).hasSize(1);
        assertThat(response.workOrders().getFirst().id()).isEqualTo(workOrderId);
        assertThat(workOrder.getEstimate()).isEqualTo(estimate);
        verify(estimateRepository, never()).save(estimate);
    }

    @Test
    void addExistingWorkOrderAppendsWithoutReplacingExistingWorkOrders() {
        UUID estimateId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        WorkOrder existingWorkOrder = workOrder(UUID.randomUUID(), vehicleId, WorkOrderStatus.ACCEPTED);
        WorkOrder workOrderToAdd = workOrder(UUID.randomUUID(), vehicleId, WorkOrderStatus.PENDING);
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .workOrders(new ArrayList<>(List.of(existingWorkOrder)))
            .build();

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));
        when(workOrderService.findEntityWithResponseGraph(workOrderToAdd.getId())).thenReturn(workOrderToAdd);

        EstimateResponse response = estimateService.addExistingWorkOrder(estimateId, workOrderToAdd.getId());

        assertThat(response.workOrders()).extracting("id")
            .containsExactlyInAnyOrder(existingWorkOrder.getId(), workOrderToAdd.getId());
        verify(estimateRepository, never()).save(estimate);
    }

    @Test
    void addExistingWorkOrderClonesAlreadyAssociatedWorkOrder() {
        UUID estimateId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(vehicleId)
            .status(EstimateStatus.PENDING)
            .build();
        Estimate sourceEstimate = Estimate.builder().id(UUID.randomUUID()).vehicleId(vehicleId).build();
        WorkOrder workOrder = workOrder(workOrderId, vehicleId, WorkOrderStatus.PENDING);
        sourceEstimate.addWorkOrder(workOrder);
        WorkOrder clonedWorkOrder = workOrder(UUID.randomUUID(), vehicleId, WorkOrderStatus.PENDING);
        estimate.addWorkOrder(clonedWorkOrder);

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));
        when(workOrderService.findEntityWithResponseGraph(workOrderId)).thenReturn(workOrder);
        when(workOrderService.cloneForEstimate(workOrder, estimate)).thenReturn(clonedWorkOrder);

        EstimateResponse response = estimateService.addExistingWorkOrder(estimateId, workOrderId);

        assertThat(response.workOrders()).extracting("id").containsExactly(clonedWorkOrder.getId());
        verify(workOrderService).cloneForEstimate(workOrder, estimate);
    }

    @Test
    void addExistingWorkOrderRejectsVehicleMismatch() {
        UUID estimateId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        Estimate estimate = Estimate.builder()
            .id(estimateId)
            .customerId(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .status(EstimateStatus.PENDING)
            .build();
        WorkOrder workOrder = workOrder(workOrderId, UUID.randomUUID(), WorkOrderStatus.PENDING);

        when(estimateRepository.findById(estimateId)).thenReturn(java.util.Optional.of(estimate));
        when(workOrderService.findEntityWithResponseGraph(workOrderId)).thenReturn(workOrder);

        assertThatThrownBy(() -> estimateService.addExistingWorkOrder(estimateId, workOrderId))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Work order vehicleId must match estimate vehicleId");
    }

    private WorkOrder workOrder(WorkOrderStatus status) {
        return workOrder(UUID.randomUUID(), UUID.randomUUID(), status);
    }

    private WorkOrder workOrder(UUID vehicleId, WorkOrderStatus status) {
        return workOrder(UUID.randomUUID(), vehicleId, status);
    }

    private WorkOrder workOrder(UUID id, UUID vehicleId, WorkOrderStatus status) {
        Part part = Part.builder()
            .id(UUID.randomUUID())
            .sku(41002)
            .manufacturer("Denso")
            .name("Iridium Spark Plug")
            .price(new BigDecimal("20.00"))
            .build();
        WorkOrder workOrder = WorkOrder.builder()
            .id(id)
            .vehicleId(vehicleId)
            .status(status)
            .summary("Replace spark plugs")
            .notes("Customer approved premium plugs.")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("2.00"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        WorkOrderPart workOrderPart = WorkOrderPart.builder()
            .id(UUID.randomUUID())
            .workOrder(workOrder)
            .part(part)
            .quantity(2)
            .build();
        workOrder.setPartsNeeded(List.of(workOrderPart));
        return workOrder;
    }
}
