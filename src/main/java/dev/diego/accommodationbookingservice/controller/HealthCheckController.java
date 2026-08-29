package dev.diego.accommodationbookingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check Controller", description = "Endpoint to check application status")
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @Operation(summary = "Check if application is running",
            description = "Returns a simple status message")
    @GetMapping
    public String checkHealth() {
        return "Application is up and running!";
    }
}
