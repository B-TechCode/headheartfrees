package com.headheartfrees.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public liveness endpoint, used by docker-compose healthchecks and by CI to
 * confirm the application boots.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service liveness")
public class HealthController {

    private final String version;

    HealthController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping
    @Operation(summary = "Report that the service is up")
    public HealthResponse health() {
        return new HealthResponse("UP", version, Instant.now());
    }
}
