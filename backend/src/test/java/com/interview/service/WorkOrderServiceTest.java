package com.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.dto.WorkOrderPartRequest;
import com.interview.dto.WorkOrderRequest;
import com.interview.dto.WorkOrderResponse;
import com.interview.dto.WorkOrderUpdateRequest;
import com.interview.entity.Estimate;
import com.interview.entity.Part;
import com.interview.entity.WorkOrder;
import com.interview.entity.WorkOrderStatus;
import com.interview.exception.InvalidRequestException;
import com.interview.repository.PartRepository;
import com.interview.repository.WorkOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {
    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void createCalculatesLaborAndQuantityBasedPartCost() {
        UUID partId = UUID.randomUUID();
        WorkOrderRequest request = workOrderRequest(partId);

        when(partRepository.findAllById(Set.of(partId))).thenReturn(List.of(part(partId, new BigDecimal("12.50"))));
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(UUID.randomUUID());
            return workOrder;
        });

        WorkOrderResponse response = workOrderService.create(request);

        assertThat(response.summary()).isEqualTo("Replace spark plugs");
        assertThat(response.notes()).isEqualTo("Customer approved premium plugs.");
        assertThat(response.laborCost()).isEqualByComparingTo("250.00");
        assertThat(response.partsNeeded().getFirst().totalCost()).isEqualByComparingTo("50.00");
        assertThat(response.totalCost()).isEqualByComparingTo("300.00");
    }

    @Test
    void createConsolidatesDuplicatePartQuantities() {
        UUID partId = UUID.randomUUID();
        WorkOrderRequest request = new WorkOrderRequest(
            UUID.randomUUID(),
            WorkOrderStatus.PENDING,
            "Replace spark plugs",
            "Customer approved premium plugs.",
            new BigDecimal("100.00"),
            new BigDecimal("1.00"),
            List.of(new WorkOrderPartRequest(partId, 1), new WorkOrderPartRequest(partId, 2))
        );

        when(partRepository.findAllById(Set.of(partId))).thenReturn(List.of(part(partId, new BigDecimal("12.50"))));
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(UUID.randomUUID());
            return workOrder;
        });

        WorkOrderResponse response = workOrderService.create(request);

        assertThat(response.partsNeeded()).hasSize(1);
        assertThat(response.partsNeeded().getFirst().quantity()).isEqualTo(3);
        assertThat(response.partsNeeded().getFirst().totalCost()).isEqualByComparingTo("37.50");
        assertThat(response.totalCost()).isEqualByComparingTo("137.50");
    }

    @Test
    void createAllowsLaborOnlyWorkOrder() {
        WorkOrderRequest request = new WorkOrderRequest(
            UUID.randomUUID(),
            WorkOrderStatus.PENDING,
            "Perform diagnostic inspection",
            "Labor-only inspection.",
            new BigDecimal("150.00"),
            new BigDecimal("0.75"),
            List.of()
        );

        when(partRepository.findAllById(Set.of())).thenReturn(List.of());
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(UUID.randomUUID());
            return workOrder;
        });

        WorkOrderResponse response = workOrderService.create(request);

        assertThat(response.partsNeeded()).isEmpty();
        assertThat(response.totalCost()).isEqualByComparingTo("112.50");
    }

    @Test
    void createRejectsMissingPartFromBulkLookup() {
        UUID partId = UUID.randomUUID();
        WorkOrderRequest request = workOrderRequest(partId);

        when(partRepository.findAllById(Set.of(partId))).thenReturn(List.of());

        assertThatThrownBy(() -> workOrderService.create(request))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Part " + partId + " does not exist");
    }

    @Test
    void updateUsesDirtyCheckingWithoutCallingSave() {
        UUID workOrderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        WorkOrder workOrder = WorkOrder.builder()
            .id(workOrderId)
            .vehicleId(vehicleId)
            .status(WorkOrderStatus.PENDING)
            .summary("Old summary")
            .notes("Old notes")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("1.00"))
            .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(workOrder));
        when(partRepository.findAllById(Set.of(partId))).thenReturn(List.of(part(partId, new BigDecimal("12.50"))));

        WorkOrderResponse response = workOrderService.update(workOrderId, workOrderUpdateRequest(partId));

        assertThat(response.summary()).isEqualTo("Replace spark plugs");
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        verify(workOrderRepository, never()).save(workOrder);
    }

    @Test
    void deleteClearsEstimateAssociationBeforeDeletingWorkOrder() {
        UUID workOrderId = UUID.randomUUID();
        Estimate estimate = Estimate.builder()
            .id(UUID.randomUUID())
            .vehicleId(UUID.randomUUID())
            .build();
        WorkOrder workOrder = WorkOrder.builder()
            .id(workOrderId)
            .vehicleId(estimate.getVehicleId())
            .status(WorkOrderStatus.PENDING)
            .summary("Replace spark plugs")
            .laborRate(new BigDecimal("100.00"))
            .laborTime(new BigDecimal("1.00"))
            .build();
        estimate.addWorkOrder(workOrder);

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(workOrder));

        workOrderService.delete(workOrderId);

        assertThat(workOrder.getEstimate()).isNull();
        assertThat(estimate.getWorkOrders()).doesNotContain(workOrder);
        verify(workOrderRepository).delete(workOrder);
    }

    private WorkOrderRequest workOrderRequest(UUID partId) {
        return new WorkOrderRequest(
            UUID.randomUUID(),
            WorkOrderStatus.PENDING,
            "Replace spark plugs",
            "Customer approved premium plugs.",
            new BigDecimal("100.00"),
            new BigDecimal("2.50"),
            List.of(new WorkOrderPartRequest(partId, 4))
        );
    }

    private WorkOrderUpdateRequest workOrderUpdateRequest(UUID partId) {
        return new WorkOrderUpdateRequest(
            WorkOrderStatus.PENDING,
            "Replace spark plugs",
            "Customer approved premium plugs.",
            new BigDecimal("100.00"),
            new BigDecimal("2.50"),
            List.of(new WorkOrderPartRequest(partId, 4))
        );
    }

    private Part part(UUID id, BigDecimal price) {
        return Part.builder()
            .id(id)
            .sku(12345)
            .manufacturer("Denso")
            .name("Iridium Spark Plug")
            .price(price)
            .build();
    }
}
