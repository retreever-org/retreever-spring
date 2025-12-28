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
import org.springframework.util.ClassUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        log.info("Initializing Retreever. Resolving API Documentation.");

        ApplicationContext context = event.getApplicationContext();
        Class<?> appClass = event.getSpringApplication().getMainApplicationClass();

        // Scan for @RestController-annotated classes
        Set<Class<?>> allControllers = ControllerScanner.scanControllers(context);
        Set<Class<?>> allAdvices = ControllerScanner.scanControllerAdvices(context);

        // Get base packages
        List<String> basePackages = orchestrator.getBasePackages();

        // Filter to base packages only
        Set<Class<?>> controllers = filterByBasePackages(allControllers, basePackages);
        Set<Class<?>> controllerAdvices = filterByBasePackages(allAdvices, basePackages);

        // Build final documentation snapshot
        this.cached = orchestrator.build(appClass, controllers, controllerAdvices);

        log.info("Retreever initialized. API Document Ready.");
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

    private Set<Class<?>> filterByBasePackages(Set<Class<?>> classes, List<String> basePackages) {
        return classes.stream()
                .filter(clazz -> isInBasePackages(clazz, basePackages))
                .collect(Collectors.toSet());
    }

    private boolean isInBasePackages(Class<?> clazz, List<String> basePackages) {
        String pkg = ClassUtils.getPackageName(clazz);
        return basePackages.stream().anyMatch(pkg::startsWith);
    }
}
