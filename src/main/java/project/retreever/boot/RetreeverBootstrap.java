/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.boot;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import project.retreever.engine.ControllerScanner;
import project.retreever.engine.RetreeverOrchestrator;
import project.retreever.view.dto.ApiDocument;

import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bootstrap component responsible for building and caching the API document
 * once the Spring application is fully initialized.
 */
@Component
public class RetreeverBootstrap {

    private final Logger log = Logger.getLogger(RetreeverBootstrap.class.getName());

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
        log.info("Initializing Retreever API Document...");

        ApplicationContext context = event.getApplicationContext();
        Class<?> appClass = event.getSpringApplication().getMainApplicationClass();

        // Scan for @RestController-annotated classes
        Set<Class<?>> controllers = ControllerScanner.scanControllers(context);

        // Build final documentation snapshot
        this.cached = orchestrator.build(appClass, controllers);

        log.info("✅ Retreever API Document built successfully.");
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
