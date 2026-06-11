package com.interview.repository;

import com.interview.entity.WorkOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {
    @Query("""
        SELECT DISTINCT workOrder
        FROM WorkOrder workOrder
        LEFT JOIN FETCH workOrder.partsNeeded workOrderPart
        LEFT JOIN FETCH workOrderPart.part
        LEFT JOIN FETCH workOrder.estimate
        WHERE workOrder.id = :id
        """)
    WorkOrder findByIdWithResponseGraph(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT workOrder
        FROM WorkOrder workOrder
        LEFT JOIN FETCH workOrder.partsNeeded workOrderPart
        LEFT JOIN FETCH workOrderPart.part
        LEFT JOIN FETCH workOrder.estimate
        WHERE workOrder.id IN :ids
        """)
    List<WorkOrder> findAllWithPartsAndEstimateIncluded(@Param("ids") List<UUID> ids);
}
