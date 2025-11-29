/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.boot;

import dev.retreever.engine.RetreeverOrchestrator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * Auto-configures all Retreever components using component scanning.
 */
@AutoConfiguration
@ComponentScan(basePackages = "dev.retreever")
public class RetreeverAutoConfiguration {

    @Bean
    public RetreeverOrchestrator orchestrator(ApplicationContext context) {

        // Find the @SpringBootApplication class
        String[] appBeans = context.getBeanNamesForAnnotation(SpringBootApplication.class);

        if (appBeans.length == 0) {
            // fallback — but extremely unlikely
            return new RetreeverOrchestrator(List.of());
        }

        Class<?> appClass = context.getType(appBeans[0]);
        if (appClass == null || appClass.getPackage() == null) {
            return new RetreeverOrchestrator(List.of());
        }

        String basePackage = appClass.getPackage().getName();

        return new RetreeverOrchestrator(List.of(basePackage, "java.util"));
    }
}
