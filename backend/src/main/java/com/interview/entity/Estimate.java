package com.interview.entity;

import com.interview.dto.EstimateRequest;
import com.interview.dto.EstimateResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "estimates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Estimate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstimateStatus status;

    @OneToMany(mappedBy = "estimate", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkOrder> workOrders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Estimate from(EstimateRequest request) {
        Estimate estimate = new Estimate();
        estimate.setCustomerId(request.customerId());
        estimate.setVehicleId(request.vehicleId());
        estimate.setStatus(EstimateStatus.PENDING);
        return estimate;
    }

    public BigDecimal getTotalCost() {
        return totalCost(workOrders);
    }

    public BigDecimal getTotalTime() {
        return totalTime(workOrders);
    }

    public boolean containsWorkOrder(UUID workOrderId) {
        return workOrders.stream().anyMatch(workOrder -> workOrder.getId().equals(workOrderId));
    }

    public EstimateResponse toResponse() {
        return toResponse(workOrders);
    }

    public EstimateResponse toResponse(List<WorkOrder> responseWorkOrders) {
        return new EstimateResponse(
            id,
            customerId,
            vehicleId,
            status,
            totalCost(responseWorkOrders),
            totalTime(responseWorkOrders),
            createdAt,
            updatedAt,
            responseWorkOrders.stream()
                .sorted(refusedLastThenCreatedAt())
                .map(WorkOrder::toSummaryResponse)
                .toList()
        );
    }

    public void addWorkOrder(WorkOrder workOrder) {
        workOrders.add(workOrder);
        workOrder.setEstimate(this);
    }

    public void removeWorkOrder(WorkOrder workOrder) {
        workOrders.remove(workOrder);
        workOrder.setEstimate(null);
    }

    public void clearWorkOrders() {
        workOrders.forEach(workOrder -> workOrder.setEstimate(null));
        workOrders.clear();
    }

    private static Comparator<WorkOrder> refusedLastThenCreatedAt() {
        return Comparator.comparing((WorkOrder workOrder) -> workOrder.getStatus().getSortPriority())
            .thenComparing(WorkOrder::getCreatedAt);
    }

    private static BigDecimal totalCost(List<WorkOrder> responseWorkOrders) {
        return responseWorkOrders.stream()
            .filter(workOrder -> workOrder.getStatus() != WorkOrderStatus.REFUSED)
            .map(WorkOrder::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal totalTime(List<WorkOrder> responseWorkOrders) {
        return responseWorkOrders.stream()
            .filter(workOrder -> workOrder.getStatus() != WorkOrderStatus.REFUSED)
            .map(WorkOrder::getLaborTime)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }
}
