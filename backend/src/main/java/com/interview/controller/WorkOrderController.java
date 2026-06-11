package com.interview.controller;

import com.interview.dto.PageResponse;
import com.interview.dto.WorkOrderRequest;
import com.interview.dto.WorkOrderResponse;
import com.interview.dto.WorkOrderUpdateRequest;
import com.interview.entity.WorkOrderStatus;
import com.interview.service.WorkOrderService;
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
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders", description = "Create and manage repair work orders")
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a work order", description = "Creates a standalone work order with labor and quantity-based parts.")
    public WorkOrderResponse create(@Valid @RequestBody WorkOrderRequest request) {
        return workOrderService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a work order", description = "Returns work-order details, including needed parts.")
    public WorkOrderResponse get(@PathVariable UUID id) {
        return workOrderService.get(id);
    }

    @GetMapping
    @Operation(summary = "List work orders", description = "Returns paginated work orders, optionally filtered by status.")
    public PageResponse<WorkOrderResponse> list(
        @RequestParam(required = false) WorkOrderStatus status,
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
        @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size cannot exceed 100") int size
    ) {
        return workOrderService.list(status, page, size);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a work order", description = "Replaces editable work-order details and part quantities.")
    public WorkOrderResponse update(@PathVariable UUID id, @Valid @RequestBody WorkOrderUpdateRequest request) {
        return workOrderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a work order")
    public void delete(@PathVariable UUID id) {
        workOrderService.delete(id);
    }
}
