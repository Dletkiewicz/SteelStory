package com.steelstory.bootstrap.web;

import com.steelstory.bootstrap.web.dto.StatusResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @Operation(
            operationId = "getStatus",
            summary = "Check API connectivity",
            description = "Public connectivity endpoint. Database readiness is reported by Actuator health."
    )
    @GetMapping(value = "/api/v1/status", produces = "application/json")
    public ResponseEntity<StatusResponseDto> status() {
        return ResponseEntity.ok(new StatusResponseDto("steelstory", "UP"));
    }
}
