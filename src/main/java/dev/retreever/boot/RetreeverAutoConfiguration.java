/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.retreever.api.config.RetreeverCorsFilter;
import dev.retreever.auth.RetreeverAuthenticationFilter;
import dev.retreever.auth.RetreeverAuthProperties;
import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.auth.RetreeverTokenService;
import dev.retreever.config.RetreeverCorsProperties;
import dev.retreever.endpoint.model.ApiHeader;
import dev.retreever.engine.RetreeverOrchestrator;
import dev.retreever.schema.resolver.jackson.JsonNameResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

/**
 * Auto-configures all Retreever components using component scanning.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "retreever", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "dev.retreever")
public class RetreeverAutoConfiguration {

    @Bean
    public RetreeverOrchestrator orchestrator(
            ApplicationContext context,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {

        JsonNameResolver.configure(objectMapperProvider.getIfAvailable());

        // Find the @SpringBootApplication class
        String[] appBeans = context.getBeanNamesForAnnotation(SpringBootApplication.class);
        Map<String, ApiHeader> headerBeans = context.getBeansOfType(ApiHeader.class);
        var headers = headerBeans.values().stream().toList();

        if (appBeans.length == 0) {
            // fallback — but extremely unlikely
            return new RetreeverOrchestrator(List.of(), headers);
        }

        Class<?> appClass = context.getType(appBeans[0]);
        if (appClass == null || appClass.getPackage() == null) {
            return new RetreeverOrchestrator(List.of(), headers);
        }

        String basePackage = appClass.getPackage().getName();

        return new RetreeverOrchestrator(List.of(basePackage, "java.util"), headers);
    }

    @Bean
    public RetreeverAuthenticationFilter retreeverAuthenticationFilter(
            RetreeverAuthProperties authProperties,
            RetreeverTokenService tokenService) {
        return new RetreeverAuthenticationFilter(authProperties, tokenService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "retreever.dev", name = "allow-cross-origin")
    public RetreeverCorsFilter retreeverCorsFilter(RetreeverCorsProperties corsProperties) {
        return new RetreeverCorsFilter(corsProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "retreever.dev", name = "allow-cross-origin")
    public FilterRegistrationBean<RetreeverCorsFilter> retreeverCorsFilterRegistration(
            RetreeverCorsFilter corsFilter) {
        FilterRegistrationBean<RetreeverCorsFilter> registration = new FilterRegistrationBean<>(corsFilter);

        registration.setName("retreeverCorsFilter");
        registration.addUrlPatterns(
                "/retreever",
                "/retreever/*",
                "/assets/*",
                "/images/*",
                "/index.html",
                "/manifest.json",
                "/sw.js",
                "/favicon.ico"
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<RetreeverAuthenticationFilter> retreeverAuthenticationFilterRegistration(
            RetreeverAuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<RetreeverAuthenticationFilter> registration =
                new FilterRegistrationBean<>(authenticationFilter);

        registration.setName("retreeverAuthenticationFilter");
        registration.addUrlPatterns(
                RetreeverAuthSupport.DOC_PATH,
                RetreeverAuthSupport.PING_PATH,
                RetreeverAuthSupport.ENVIRONMENT_PATH
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);

        return registration;
    }
}
