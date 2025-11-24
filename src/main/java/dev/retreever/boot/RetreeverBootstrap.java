/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dev.retreever.engine.ControllerScanner;
import dev.retreever.engine.RetreeverOrchestrator;
import dev.retreever.view.dto.ApiDocument;

import java.time.Instant;
import java.util.Set;

/**
 * Bootstrap component responsible for building and caching the API document
 * once the Spring application is fully initialized.
 */
@Component
public class RetreeverBootstrap {

    private final Logger log = LoggerFactory.getLogger(RetreeverBootstrap.class);

    private final RetreeverOrchestrator orchestrator;
    private ApiDocument cached;

    public RetreeverBootstrap(RetreeverOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Triggered on {@link ApplicationReadyEvent}. Discovers controllers,
     * builds the API document using the Retreever pipeline, and caches it.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        log.debug("Initializing Retreever...");

        ApplicationContext context = event.getApplicationContext();
        Class<?> appClass = event.getSpringApplication().getMainApplicationClass();

        // Scan for @RestController-annotated classes
        Set<Class<?>> controllers = ControllerScanner.scanControllers(context);
        Set<Class<?>> controllerAdvices = ControllerScanner.scanControllerAdvices(context);

        // Build final documentation snapshot
        this.cached = orchestrator.build(appClass, controllers, controllerAdvices);

        log.info("Retreever initialized.");
    }

    /**
     * Returns the cached API document.
     */
    public ApiDocument getDocument() {
        return cached;
    }

    /**
     * Returns the timestamp when the API document was built.
     */
    public Instant getUptime() {
        return cached.upTime();
    }
}
