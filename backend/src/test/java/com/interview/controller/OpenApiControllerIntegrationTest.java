package com.interview.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsExposeEstimateBuilderMetadataAndPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title", is("Tekmetric Estimate Builder API")))
            .andExpect(jsonPath("$.tags[*].name", hasItem("Estimates")))
            .andExpect(jsonPath("$.paths['/api/estimates/{estimateId}/work-orders']").exists());
    }
}
