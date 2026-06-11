package com.interview.controller;

import com.interview.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Application health checks")
public class HealthController {
    @GetMapping("/api/health")
    @Operation(summary = "Check API health")
    public HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }
}
