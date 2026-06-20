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
import dev.retreever.api.config.RetreeverSecurityHeadersFilter;
import dev.retreever.auth.RetreeverAuthenticationFilter;
import dev.retreever.auth.RetreeverAuthenticationService;
import dev.retreever.auth.RetreeverAuthProperties;
import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.auth.RetreeverTokenService;
import dev.retreever.config.RetreeverCorsProperties;
import dev.retreever.config.RetreeverDocumentationExclusionProperties;
import dev.retreever.config.RetreeverStudioProperties;
import dev.retreever.endpoint.model.ApiHeader;
import dev.retreever.engine.RetreeverOrchestrator;
import dev.retreever.schema.resolver.jackson.JsonNameResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.util.StringValueResolver;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

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
            ObjectProvider<ObjectMapper> objectMapperProvider,
            RetreeverDocumentationExclusionProperties exclusionProperties,
            RetreeverAuthProperties authProperties,
            RetreeverAuthenticationService authenticationService,
            RetreeverStudioProperties studioProperties
    ) {

        JsonNameResolver.configure(objectMapperProvider.getIfAvailable());

        // Find the @SpringBootApplication class
        String[] appBeans = context.getBeanNamesForAnnotation(SpringBootApplication.class);
        Map<String, ApiHeader> headerBeans = context.getBeansOfType(ApiHeader.class);
        var headers = headerBeans.values().stream().toList();
        StringValueResolver valueResolver = mappingValueResolver(context);

        if (appBeans.length == 0) {
            // fallback — but extremely unlikely
            return new RetreeverOrchestrator(
                    List.of(),
                    headers,
                    exclusionProperties,
                    authProperties,
                    authenticationService,
                    studioProperties,
                    valueResolver
            );
        }

        Class<?> appClass = context.getType(appBeans[0]);
        if (appClass == null || appClass.getPackage() == null) {
            return new RetreeverOrchestrator(
                    List.of(),
                    headers,
                    exclusionProperties,
                    authProperties,
                    authenticationService,
                    studioProperties,
                    valueResolver
            );
        }

        String basePackage = appClass.getPackage().getName();

        return new RetreeverOrchestrator(
                List.of(basePackage, "java.util"),
                headers,
                exclusionProperties,
                authProperties,
                authenticationService,
                studioProperties,
                valueResolver
        );
    }

    private StringValueResolver mappingValueResolver(ApplicationContext context) {
        return value -> {
            if (value == null) {
                return null;
            }

            try {
                if (context instanceof ConfigurableApplicationContext configurableContext) {
                    String embedded = configurableContext.getBeanFactory().resolveEmbeddedValue(value);
                    if (embedded != null) {
                        return embedded;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to Environment resolution, which preserves unresolved placeholders.
            }
            return context.getEnvironment().resolvePlaceholders(value);
        };
    }

    @Bean
    public RetreeverSecurityHeadersFilter retreeverSecurityHeadersFilter() {
        return new RetreeverSecurityHeadersFilter();
    }

    @Bean
    public FilterRegistrationBean<RetreeverSecurityHeadersFilter> retreeverSecurityHeadersFilterRegistration(
            RetreeverSecurityHeadersFilter securityHeadersFilter,
            RetreeverBasePathResolver basePathResolver) {
        FilterRegistrationBean<RetreeverSecurityHeadersFilter> registration =
                new FilterRegistrationBean<>(securityHeadersFilter);

        registration.setName("retreeverSecurityHeadersFilter");
        registration.addUrlPatterns(retreeverFilterPatterns(basePathResolver.resolveFilterBasePath()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);

        return registration;
    }

    @Bean
    public RetreeverAuthenticationFilter retreeverAuthenticationFilter(
            RetreeverAuthenticationService authenticationService,
            RetreeverTokenService tokenService) {
        return new RetreeverAuthenticationFilter(authenticationService, tokenService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "retreever.dev", name = "allow-cross-origin")
    public RetreeverCorsFilter retreeverCorsFilter(RetreeverCorsProperties corsProperties) {
        return new RetreeverCorsFilter(corsProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "retreever.dev", name = "allow-cross-origin")
    public FilterRegistrationBean<RetreeverCorsFilter> retreeverCorsFilterRegistration(
            RetreeverCorsFilter corsFilter,
            RetreeverBasePathResolver basePathResolver) {
        FilterRegistrationBean<RetreeverCorsFilter> registration = new FilterRegistrationBean<>(corsFilter);

        registration.setName("retreeverCorsFilter");
        registration.addUrlPatterns(retreeverFilterPatterns(basePathResolver.resolveFilterBasePath()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<RetreeverAuthenticationFilter> retreeverAuthenticationFilterRegistration(
            RetreeverAuthenticationFilter authenticationFilter,
            RetreeverBasePathResolver basePathResolver) {
        FilterRegistrationBean<RetreeverAuthenticationFilter> registration =
                new FilterRegistrationBean<>(authenticationFilter);

        registration.setName("retreeverAuthenticationFilter");
        registration.addUrlPatterns(retreeverProtectedApiPatterns(basePathResolver.resolveFilterBasePath()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);

        return registration;
    }

    private String[] retreeverFilterPatterns(String basePath) {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        patterns.add(RetreeverAuthSupport.RETREEVER_BASE_PATH);
        patterns.add(RetreeverAuthSupport.RETREEVER_BASE_PATH + "/*");
        patterns.add(basePath);
        patterns.add(basePath + "/*");
        return patterns.toArray(String[]::new);
    }

    private String[] retreeverProtectedApiPatterns(String basePath) {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();
        patterns.add(RetreeverAuthSupport.DOC_PATH);
        patterns.add(RetreeverAuthSupport.PING_PATH);
        patterns.add(RetreeverAuthSupport.ENVIRONMENT_PATH);
        patterns.add(basePath + "/doc");
        patterns.add(basePath + "/ping");
        patterns.add(basePath + "/environment");
        return patterns.toArray(String[]::new);
    }
}
