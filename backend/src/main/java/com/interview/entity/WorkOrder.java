package com.interview.entity;

import com.interview.dto.WorkOrderRequest;
import com.interview.dto.WorkOrderResponse;
import com.interview.dto.WorkOrderSummaryResponse;
import com.interview.dto.WorkOrderUpdateRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @Column(nullable = false, length = 150)
    private String summary;

    @Column(length = 1000)
    private String notes;

    @Column(name = "labor_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborRate;

    @Column(name = "labor_time", nullable = false, precision = 8, scale = 2)
    private BigDecimal laborTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id")
    private Estimate estimate;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkOrderPart> partsNeeded = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static WorkOrder from(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setVehicleId(request.vehicleId());
        workOrder.updateFrom(request.toUpdateRequest());
        return workOrder;
    }

    public void updateFrom(WorkOrderUpdateRequest request) {
        status = request.status();
        summary = request.summary();
        notes = request.notes();
        laborRate = request.laborRate();
        laborTime = request.laborTime();
    }

    public void replacePartsNeeded(List<WorkOrderPart> replacementParts) {
        Set<UUID> replacementPartIds = replacementParts.stream()
            .map(workOrderPart -> workOrderPart.getPart().getId())
            .collect(Collectors.toSet());

        partsNeeded.removeIf(workOrderPart -> !replacementPartIds.contains(workOrderPart.getPart().getId()));
        replacementParts.forEach(workOrderPart -> {
            partsNeeded.stream()
                .filter(existingPart -> existingPart.getPart().getId().equals(workOrderPart.getPart().getId()))
                .findFirst()
                .ifPresentOrElse(
                    existingPart -> existingPart.setQuantity(workOrderPart.getQuantity()),
                    () -> {
                        workOrderPart.setWorkOrder(this);
                        partsNeeded.add(workOrderPart);
                    }
                );
        });
    }

    public WorkOrder copyForEstimate(Estimate targetEstimate) {
        WorkOrder clone = WorkOrder.builder()
            .vehicleId(vehicleId)
            .status(status)
            .summary(summary)
            .notes(notes)
            .laborRate(laborRate)
            .laborTime(laborTime)
            .build();

        List<WorkOrderPart> clonedParts = partsNeeded.stream()
            .map(workOrderPart -> workOrderPart.copyForWorkOrder(clone))
            .toList();
        clone.replacePartsNeeded(clonedParts);
        targetEstimate.addWorkOrder(clone);
        return clone;
    }

    public BigDecimal calculateLaborCost() {
        return money(laborRate.multiply(laborTime));
    }

    public BigDecimal getTotalCost() {
        BigDecimal partsCost = partsNeeded.stream()
            .map(WorkOrderPart::calculateCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return money(calculateLaborCost().add(partsCost));
    }

    public WorkOrderResponse toResponse() {
        return new WorkOrderResponse(
            id,
            vehicleId,
            status,
            summary,
            notes,
            laborRate,
            laborTime,
            calculateLaborCost(),
            getTotalCost(),
            estimateUrl(),
            createdAt,
            updatedAt,
            partsNeeded.stream()
                .sorted(Comparator.comparing(workOrderPart -> workOrderPart.getPart().getName()))
                .map(WorkOrderPart::toResponse)
                .toList()
        );
    }

    public WorkOrderSummaryResponse toSummaryResponse() {
        return new WorkOrderSummaryResponse(
            id,
            vehicleId,
            status,
            summary,
            notes,
            laborRate,
            laborTime,
            calculateLaborCost(),
            getTotalCost(),
            estimateUrl(),
            createdAt,
            updatedAt
        );
    }

    private String estimateUrl() {
        return estimate == null ? null : "/api/estimates/" + estimate.getId();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
