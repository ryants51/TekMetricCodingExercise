package com.interview.service;

import com.interview.config.MdcKeys;
import com.interview.dto.PageResponse;
import com.interview.dto.WorkOrderPartRequest;
import com.interview.dto.WorkOrderRequest;
import com.interview.dto.WorkOrderResponse;
import com.interview.dto.WorkOrderUpdateRequest;
import com.interview.entity.Part;
import com.interview.entity.WorkOrder;
import com.interview.entity.WorkOrderPart;
import com.interview.entity.WorkOrderStatus;
import com.interview.exception.InvalidRequestException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.repository.PartRepository;
import com.interview.repository.WorkOrderRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder savedWorkOrder = createWorkOrderFromRequest(request);
        MDC.put(MdcKeys.WORK_ORDER_ID, savedWorkOrder.getId().toString());
        return savedWorkOrder.toResponse();
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(UUID id) {
        return findEntityWithResponseGraph(id).toResponse();
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> list(WorkOrderStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<WorkOrder> workOrders = workOrderRepository.findAll(withFilters(status), pageRequest);
        Map<UUID, WorkOrder> workOrdersById = findWorkOrdersWithPartsAndEstimateIncluded(workOrders);
        Page<WorkOrderResponse> responsePage = workOrders.map(workOrder -> workOrdersById.get(workOrder.getId()).toResponse());

        return PageResponse.from(responsePage);
    }

    @Transactional
    public WorkOrderResponse update(UUID id, WorkOrderUpdateRequest request) {
        MDC.put(MdcKeys.WORK_ORDER_ID, id.toString());
        WorkOrder workOrder = findEntity(id);
        updateWorkOrderFromRequest(workOrder, request);
        return workOrder.toResponse();
    }

    @Transactional
    public void delete(UUID id) {
        MDC.put(MdcKeys.WORK_ORDER_ID, id.toString());
        WorkOrder workOrder = findEntity(id);
        if (workOrder.getEstimate() != null) {
            workOrder.getEstimate().removeWorkOrder(workOrder);
        }
        workOrderRepository.delete(workOrder);
    }

    @Transactional(readOnly = true)
    public WorkOrder findEntity(UUID id) {
        return workOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Work order " + id + " was not found"));
    }

    @Transactional(readOnly = true)
    public WorkOrder findEntityWithResponseGraph(UUID id) {
        WorkOrder workOrder = workOrderRepository.findByIdWithResponseGraph(id);
        if (workOrder == null) {
            throw new ResourceNotFoundException("Work order " + id + " was not found");
        }
        return workOrder;
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findAllWithPartsAndEstimateIncluded(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return workOrderRepository.findAllWithPartsAndEstimateIncluded(ids);
    }

    @Transactional
    public WorkOrder createWorkOrderFromRequest(WorkOrderRequest request) {
        WorkOrder workOrder = WorkOrder.from(request);
        updatePartsNeeded(workOrder, request.partsNeeded());
        return workOrderRepository.saveAndFlush(workOrder);
    }

    @Transactional
    public WorkOrder cloneForEstimate(WorkOrder sourceWorkOrder, com.interview.entity.Estimate targetEstimate) {
        WorkOrder clone = sourceWorkOrder.copyForEstimate(targetEstimate);
        return workOrderRepository.saveAndFlush(clone);
    }

    private void updateWorkOrderFromRequest(WorkOrder workOrder, WorkOrderUpdateRequest request) {
        workOrder.updateFrom(request);
        updatePartsNeeded(workOrder, request.partsNeeded());
    }

    private void updatePartsNeeded(WorkOrder workOrder, List<WorkOrderPartRequest> partsNeeded) {
        List<WorkOrderPartRequest> consolidatedPartsNeeded = consolidatePartsNeeded(partsNeeded == null ? List.of() : partsNeeded);
        Map<UUID, Part> partsById = findPartsById(consolidatedPartsNeeded);
        List<WorkOrderPart> replacementParts = consolidatedPartsNeeded.stream()
            .map(partRequest -> WorkOrderPart.from(workOrder, partsById.get(partRequest.partId()), partRequest))
            .toList();
        workOrder.replacePartsNeeded(replacementParts);
    }

    private Map<UUID, Part> findPartsById(List<WorkOrderPartRequest> partsNeeded) {
        Set<UUID> requestedPartIds = partsNeeded.stream()
            .map(WorkOrderPartRequest::partId)
            .collect(Collectors.toSet());

        Map<UUID, Part> partsById = partRepository.findAllById(requestedPartIds).stream()
            .collect(Collectors.toMap(Part::getId, Function.identity()));

        List<UUID> missingPartIds = requestedPartIds.stream()
            .filter(id -> !partsById.containsKey(id))
            .sorted()
            .toList();

        if (!missingPartIds.isEmpty()) {
            UUID missingPartId = missingPartIds.getFirst();
            MDC.put(MdcKeys.PART_ID, missingPartId.toString());
            throw new InvalidRequestException("Part " + missingPartId + " does not exist");
        }

        return partsById;
    }

    private List<WorkOrderPartRequest> consolidatePartsNeeded(List<WorkOrderPartRequest> partsNeeded) {
        Map<UUID, Integer> quantitiesByPartId = new LinkedHashMap<>();
        for (WorkOrderPartRequest partRequest : partsNeeded) {
            quantitiesByPartId.merge(partRequest.partId(), partRequest.quantity(), Integer::sum);
        }

        return quantitiesByPartId.entrySet().stream()
            .map(entry -> new WorkOrderPartRequest(entry.getKey(), entry.getValue()))
            .toList();
    }

    private Specification<WorkOrder> withFilters(WorkOrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (query.getResultType() != Long.class) {
                Expression<Integer> statusPriority = criteriaBuilder.<Integer>selectCase()
                    .when(criteriaBuilder.equal(root.get("status"), WorkOrderStatus.PENDING), WorkOrderStatus.PENDING.getSortPriority())
                    .when(criteriaBuilder.equal(root.get("status"), WorkOrderStatus.ACCEPTED), WorkOrderStatus.ACCEPTED.getSortPriority())
                    .otherwise(WorkOrderStatus.REFUSED.getSortPriority());
                query.orderBy(criteriaBuilder.asc(statusPriority), criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<UUID, WorkOrder> findWorkOrdersWithPartsAndEstimateIncluded(Page<WorkOrder> workOrders) {
        List<UUID> workOrderIds = workOrders.stream()
            .map(WorkOrder::getId)
            .toList();

        if (workOrderIds.isEmpty()) {
            return Map.of();
        }

        return workOrderRepository.findAllWithPartsAndEstimateIncluded(workOrderIds).stream()
            .collect(Collectors.toMap(WorkOrder::getId, Function.identity()));
    }

}
