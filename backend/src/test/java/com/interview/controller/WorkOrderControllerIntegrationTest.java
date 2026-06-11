package com.interview.controller;

import static com.interview.TestResourceLoader.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WorkOrderControllerIntegrationTest {
    private static final String WORK_ORDER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createWorkOrderCalculatesTotalCostAndReturnsGeneratedId() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/create-work-order.json")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.summary", is("Replace front brake pads")))
            .andExpect(jsonPath("$.notes", is("Pads are worn below recommended thickness.")))
            .andExpect(jsonPath("$.laborCost", is(200.00)))
            .andExpect(jsonPath("$.partsNeeded[0].quantity", is(2)))
            .andExpect(jsonPath("$.totalCost", is(379.98)))
            .andExpect(jsonPath("$.estimateUrl", nullValue()))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createWorkOrderAllowsLaborOnlyWorkOrder() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/create-labor-only-work-order.json")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.partsNeeded.length()", is(0)))
            .andExpect(jsonPath("$.totalCost", is(112.50)));
    }

    @Test
    void getWorkOrderByIdReturnsSeededRecordWithParts() throws Exception {
        mockMvc.perform(get("/api/work-orders/{id}", WORK_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(WORK_ORDER_ID)))
            .andExpect(jsonPath("$.summary", is("Replace front brake pads")))
            .andExpect(jsonPath("$.estimateUrl", is("/api/estimates/eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")))
            .andExpect(jsonPath("$.partsNeeded[0].price", is(89.99)));
    }

    @Test
    void listWorkOrdersSupportsPaginationAndStatusFilter() throws Exception {
        mockMvc.perform(get("/api/work-orders")
                .param("status", "REFUSED")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[0].status", is("REFUSED")));
    }

    @Test
    void listWorkOrdersSortsByStatusPriority() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work-orders")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();

        List<String> statuses = JsonPath.read(result.getResponse().getContentAsString(), "$.content[*].status");

        assertThat(statuses).isSortedAccordingTo((left, right) -> Integer.compare(statusPriority(left), statusPriority(right)));
    }

    @Test
    void updateWorkOrderReplacesPartsAndRecalculatesTotalCost() throws Exception {
        String id = createWorkOrderAndReturnId();

        mockMvc.perform(put("/api/work-orders/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/update-work-order.json")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vehicleId", is("99999999-9999-9999-9999-999999999999")))
            .andExpect(jsonPath("$.status", is("ACCEPTED")))
            .andExpect(jsonPath("$.summary", is("Replace ignition components")))
            .andExpect(jsonPath("$.totalCost", is(170.00)));
    }

    @Test
    void updateWorkOrderUpdatesExistingPartQuantityWithoutUniqueConstraintViolation() throws Exception {
        String id = createWorkOrderAndReturnId();

        mockMvc.perform(put("/api/work-orders/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/update-same-part-work-order.json")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.partsNeeded.length()", is(1)))
            .andExpect(jsonPath("$.partsNeeded[0].partId", is("11111111-1111-1111-1111-111111111111")))
            .andExpect(jsonPath("$.partsNeeded[0].quantity", is(3)))
            .andExpect(jsonPath("$.totalCost", is(469.97)));
    }

    @Test
    void deleteWorkOrderReturnsNoContent() throws Exception {
        String id = createWorkOrderAndReturnId();

        mockMvc.perform(delete("/api/work-orders/{id}", id))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteAssociatedWorkOrderRemovesItFromEstimate() throws Exception {
        String estimateId = createEstimateAndReturnId();
        MvcResult addResult = mockMvc.perform(post("/api/estimates/{estimateId}/work-orders", estimateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/create-work-order.json")))
            .andExpect(status().isCreated())
            .andReturn();
        String workOrderId = JsonPath.read(addResult.getResponse().getContentAsString(), "$.workOrders[0].id");

        mockMvc.perform(delete("/api/work-orders/{id}", workOrderId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/estimates/{id}", estimateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workOrders.length()", is(0)));
    }

    @Test
    void createWorkOrderReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/invalid-work-order.json")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void createWorkOrderConsolidatesDuplicatePartQuantities() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/duplicate-parts-work-order.json")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.partsNeeded.length()", is(1)))
            .andExpect(jsonPath("$.partsNeeded[0].quantity", is(3)))
            .andExpect(jsonPath("$.totalCost", is(369.97)));
    }

    private String createWorkOrderAndReturnId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/work-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("work-orders/create-work-order.json")))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createEstimateAndReturnId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/estimates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("estimates/create-empty-estimate.json")))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "PENDING" -> 1;
            case "ACCEPTED" -> 2;
            case "REFUSED" -> 3;
            default -> throw new IllegalArgumentException("Unexpected status " + status);
        };
    }
}
