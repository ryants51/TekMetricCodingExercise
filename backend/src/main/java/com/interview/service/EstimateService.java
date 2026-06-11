package com.interview.service;

import com.interview.config.MdcKeys;
import com.interview.dto.EstimateRequest;
import com.interview.dto.EstimateResponse;
import com.interview.dto.EstimateUpdateRequest;
import com.interview.dto.PageResponse;
import com.interview.dto.WorkOrderRequest;
import com.interview.entity.Estimate;
import com.interview.entity.EstimateStatus;
import com.interview.entity.WorkOrder;
import com.interview.exception.ConflictException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.repository.EstimateRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstimateService {
    private final EstimateRepository estimateRepository;
    private final WorkOrderService workOrderService;

    @Transactional
    public EstimateResponse create(EstimateRequest request) {
        Estimate estimate = Estimate.from(request);
        Estimate savedEstimate = estimateRepository.saveAndFlush(estimate);
        MDC.put(MdcKeys.ESTIMATE_ID, savedEstimate.getId().toString());
        return savedEstimate.toResponse();
    }

    @Transactional(readOnly = true)
    public EstimateResponse get(UUID id) {
        Estimate estimate = findEntityWithWorkOrders(id);
        return estimate.toResponse(findResponseWorkOrders(estimate));
    }

    @Transactional(readOnly = true)
    public PageResponse<EstimateResponse> list(UUID customerId, EstimateStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Estimate> estimates = estimateRepository.findAll(withFilters(customerId, status), pageRequest);
        Map<UUID, Estimate> estimatesById = findEstimatesByIdWithWorkOrders(estimates);
        Map<UUID, List<WorkOrder>> workOrdersByEstimateId = findResponseWorkOrdersByEstimateId(estimatesById);

        return PageResponse.from(estimates.map(estimate -> {
            Estimate responseEstimate = estimatesById.get(estimate.getId());
            return responseEstimate.toResponse(workOrdersByEstimateId.getOrDefault(estimate.getId(), List.of()));
        }));
    }

    @Transactional
    public EstimateResponse update(UUID id, EstimateUpdateRequest request) {
        MDC.put(MdcKeys.ESTIMATE_ID, id.toString());
        Estimate estimate = findEntity(id);
        estimate.setStatus(request.status());
        return estimate.toResponse();
    }

    @Transactional
    public void delete(UUID id) {
        MDC.put(MdcKeys.ESTIMATE_ID, id.toString());
        Estimate estimate = findEntity(id);
        estimate.clearWorkOrders();
        estimateRepository.delete(estimate);
    }

    @Transactional
    public EstimateResponse addWorkOrder(UUID estimateId, WorkOrderRequest request) {
        MDC.put(MdcKeys.ESTIMATE_ID, estimateId.toString());
        Estimate estimate = findEntity(estimateId);
        validateVehicleMatch(estimate, request.vehicleId());
        WorkOrder workOrder = workOrderService.createWorkOrderFromRequest(request);
        MDC.put(MdcKeys.WORK_ORDER_ID, workOrder.getId().toString());
        estimate.addWorkOrder(workOrder);
        return estimate.toResponse();
    }

    @Transactional
    public EstimateResponse addExistingWorkOrder(UUID estimateId, UUID workOrderId) {
        MDC.put(MdcKeys.ESTIMATE_ID, estimateId.toString());
        MDC.put(MdcKeys.WORK_ORDER_ID, workOrderId.toString());
        Estimate estimate = findEntity(estimateId);
        WorkOrder workOrder = workOrderService.findEntityWithResponseGraph(workOrderId);
        validateVehicleMatch(estimate, workOrder.getVehicleId());

        if (workOrder.getEstimate() == null) {
            estimate.addWorkOrder(workOrder);
        } else {
            workOrder = workOrderService.cloneForEstimate(workOrder, estimate);
            MDC.put(MdcKeys.WORK_ORDER_ID, workOrder.getId().toString());
        }

        return estimate.toResponse();
    }

    public Estimate findEntity(UUID id) {
        return estimateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estimate " + id + " was not found"));
    }

    private Estimate findEntityWithWorkOrders(UUID id) {
        Estimate estimate = estimateRepository.findByIdWithWorkOrders(id);
        if (estimate == null) {
            throw new ResourceNotFoundException("Estimate " + id + " was not found");
        }
        return estimate;
    }

    private Map<UUID, Estimate> findEstimatesByIdWithWorkOrders(Page<Estimate> estimates) {
        List<UUID> estimateIds = estimates.stream()
            .map(Estimate::getId)
            .toList();

        if (estimateIds.isEmpty()) {
            return Map.of();
        }

        return estimateRepository.findAllByIdInWithWorkOrders(estimateIds).stream()
            .collect(Collectors.toMap(Estimate::getId, Function.identity()));
    }

    private List<WorkOrder> findResponseWorkOrders(Estimate estimate) {
        List<UUID> workOrderIds = estimate.getWorkOrders().stream()
            .map(WorkOrder::getId)
            .toList();
        return workOrderService.findAllWithPartsAndEstimateIncluded(workOrderIds);
    }

    private Map<UUID, List<WorkOrder>> findResponseWorkOrdersByEstimateId(Map<UUID, Estimate> estimatesById) {
        List<UUID> workOrderIds = estimatesById.values().stream()
            .flatMap(estimate -> estimate.getWorkOrders().stream())
            .map(WorkOrder::getId)
            .toList();

        return workOrderService.findAllWithPartsAndEstimateIncluded(workOrderIds).stream()
            .collect(Collectors.groupingBy(workOrder -> workOrder.getEstimate().getId()));
    }

    private void validateVehicleMatch(Estimate estimate, UUID workOrderVehicleId) {
        if (!estimate.getVehicleId().equals(workOrderVehicleId)) {
            throw new ConflictException("Work order vehicleId must match estimate vehicleId " + estimate.getVehicleId());
        }
    }

    private Specification<Estimate> withFilters(UUID customerId, EstimateStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

}
