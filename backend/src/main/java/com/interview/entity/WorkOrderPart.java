package com.interview.entity;

import com.interview.dto.WorkOrderPartRequest;
import com.interview.dto.WorkOrderPartResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order_parts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderPart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private Integer quantity;

    public static WorkOrderPart from(WorkOrder workOrder, Part part, WorkOrderPartRequest request) {
        return WorkOrderPart.builder()
            .workOrder(workOrder)
            .part(part)
            .quantity(request.quantity())
            .build();
    }

    public WorkOrderPart copyForWorkOrder(WorkOrder targetWorkOrder) {
        return WorkOrderPart.builder()
            .workOrder(targetWorkOrder)
            .part(part)
            .quantity(quantity)
            .build();
    }

    public BigDecimal calculateCost() {
        return part.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public WorkOrderPartResponse toResponse() {
        return new WorkOrderPartResponse(
            part.getId(),
            part.getSku(),
            part.getManufacturer(),
            part.getName(),
            part.getPrice(),
            quantity,
            calculateCost()
        );
    }
}
