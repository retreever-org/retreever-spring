/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.retreever.boot.RetreeverBootstrap;
import project.retreever.view.dto.ApiDocument;

import java.util.Map;

/**
 * Exposes Retreever's API documentation via HTTP endpoints.
 */
@RestController
@RequestMapping("/retreever-tool")
public class RetreeverController {

    private final RetreeverBootstrap bootstrap;

    public RetreeverController(RetreeverBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * Simple health/ping endpoint for checking tool availability.
     *
     * @return basic status and uptime info
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "uptime", bootstrap.getUptime()
        );
    }

    /**
     * Returns the full API documentation snapshot.
     *
     * @return the assembled API document
     */
    @GetMapping
    public ApiDocument getDoc() {
        return bootstrap.getDocument();
    }
}
