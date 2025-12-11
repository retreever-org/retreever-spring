/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.api;

import dev.retreever.config.TestEnvironmentConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.retreever.boot.RetreeverBootstrap;
import dev.retreever.view.dto.ApiDocument;

import java.util.Map;

/**
 * Exposes Retreever's API documentation via HTTP endpoints.
 */
@RestController
@RequestMapping("/retreever")
public class RetreeverController {

    private final RetreeverBootstrap bootstrap;
    private final TestEnvironmentConfig environmentConfig;

    public RetreeverController(
            RetreeverBootstrap bootstrap,
            TestEnvironmentConfig environmentConfig) {
        this.bootstrap = bootstrap;
        this.environmentConfig = environmentConfig;
    }

    /**
     * Simple health/ping endpoint for checking tool availability.
     *
     * @return basic status and uptime info
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = Map.of(
                "status", "OK",
                "uptime", bootstrap.getUptime()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the full API documentation snapshot.
     *
     * @return the assembled API document
     */
    @GetMapping("/doc")
    public ResponseEntity<ApiDocument> getDoc() {
        return ResponseEntity.ok(bootstrap.getDocument());
    }

    @GetMapping("/environment")
    public ResponseEntity<TestEnvironmentConfig> getEnvironment() {
        return ResponseEntity.ok(environmentConfig);
    }
}
