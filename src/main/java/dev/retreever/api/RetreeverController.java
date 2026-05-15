/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.api;

import dev.retreever.auth.RetreeverAuthProperties;
import dev.retreever.config.TestEnvironmentDocumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.retreever.boot.RetreeverBootstrap;
import dev.retreever.view.dto.ApiDocument;
import dev.retreever.view.dto.TestEnvironmentDocument;

import java.util.Map;

/**
 * Exposes Retreever's API documentation via HTTP endpoints.
 */
@RestController
@RequestMapping("/retreever")
public class RetreeverController {

    private final RetreeverBootstrap bootstrap;
    private final TestEnvironmentDocumentResolver environmentDocumentResolver;
    private final RetreeverAuthProperties authProperties;

    public RetreeverController(
            RetreeverBootstrap bootstrap,
            TestEnvironmentDocumentResolver environmentDocumentResolver,
            RetreeverAuthProperties authProperties) {
        this.bootstrap = bootstrap;
        this.environmentDocumentResolver = environmentDocumentResolver;
        this.authProperties = authProperties;
    }

    /**
     * Simple health/ping endpoint for checking tool availability.
     *
     * @return basic status and uptime info
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        if (!bootstrap.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "UNAVAILABLE",
                    "message", "Retreever failed during startup. Check the application logs for the full stack trace."
            ));
        }

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
        if (!bootstrap.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok(bootstrap.getDocument());
    }

    @GetMapping("/environment")
    public ResponseEntity<TestEnvironmentDocument> getEnvironment() {
        return ResponseEntity.ok(environmentDocumentResolver.resolve());
    }

    /**
     * Indicates whether Retreever's internal auth is enabled.
     *
     * @return {@code true} when username and password are configured, otherwise false
     */
    @GetMapping(path = "/secured", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> isProtected() {
        return ResponseEntity.ok(!authProperties.isDisabled());
    }
}
