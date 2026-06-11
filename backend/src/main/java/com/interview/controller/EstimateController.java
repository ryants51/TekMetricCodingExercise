package com.interview.controller;

import com.interview.dto.EstimateRequest;
import com.interview.dto.EstimateResponse;
import com.interview.dto.EstimateUpdateRequest;
import com.interview.dto.PageResponse;
import com.interview.dto.WorkOrderRequest;
import com.interview.entity.EstimateStatus;
import com.interview.service.EstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/estimates")
@RequiredArgsConstructor
@Tag(name = "Estimates", description = "Create and manage repair estimates")
public class EstimateController {
    private final EstimateService estimateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an estimate", description = "Creates an empty estimate.")
    public EstimateResponse create(@Valid @RequestBody EstimateRequest request) {
        return estimateService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an estimate", description = "Returns estimate totals and work-order summaries.")
    public EstimateResponse get(@PathVariable UUID id) {
        return estimateService.get(id);
    }

    @GetMapping
    @Operation(summary = "List estimates", description = "Returns paginated estimates filtered by customer ID and status.")
    public PageResponse<EstimateResponse> list(
        @RequestParam(required = false) UUID customerId,
        @RequestParam(required = false) EstimateStatus status,
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
        @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size cannot exceed 100") int size
    ) {
        return estimateService.list(customerId, status, page, size);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an estimate", description = "Updates estimate status.")
    public EstimateResponse update(@PathVariable UUID id, @Valid @RequestBody EstimateUpdateRequest request) {
        return estimateService.update(id, request);
    }

    @PostMapping("/{estimateId}/work-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a work order for an estimate", description = "Creates a work order and associates it with the selected estimate.")
    public EstimateResponse addWorkOrder(
        @PathVariable UUID estimateId,
        @Valid @RequestBody WorkOrderRequest request
    ) {
        return estimateService.addWorkOrder(estimateId, request);
    }

    @PostMapping("/{estimateId}/work-orders/{workOrderId}")
    @Operation(
        summary = "Associate an existing work order",
        description = "Associates an unowned work order or clones an already-owned work order onto the selected estimate."
    )
    public EstimateResponse addExistingWorkOrder(
        @PathVariable UUID estimateId,
        @PathVariable UUID workOrderId
    ) {
        return estimateService.addExistingWorkOrder(estimateId, workOrderId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an estimate")
    public void delete(@PathVariable UUID id) {
        estimateService.delete(id);
    }
}
