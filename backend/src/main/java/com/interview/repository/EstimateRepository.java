package com.interview.repository;

import com.interview.entity.Estimate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstimateRepository extends JpaRepository<Estimate, UUID>, JpaSpecificationExecutor<Estimate> {
    @Query("""
        SELECT DISTINCT estimate
        FROM Estimate estimate
        LEFT JOIN FETCH estimate.workOrders workOrder
        WHERE estimate.id = :id
        """)
    Estimate findByIdWithWorkOrders(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT estimate
        FROM Estimate estimate
        LEFT JOIN FETCH estimate.workOrders workOrder
        WHERE estimate.id IN :ids
        """)
    List<Estimate> findAllByIdInWithWorkOrders(@Param("ids") List<UUID> ids);
}
