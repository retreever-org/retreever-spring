/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.boot;

import dev.retreever.auth.RetreeverAuthenticationService;
import dev.retreever.config.RetreeverSecurityHintProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
    private final RetreeverUiLocationResolver uiLocationResolver;
    private final RetreeverAuthenticationService authenticationService;
    private final RetreeverSecurityHintProperties securityHintProperties;
    private ApiDocument cached;
    private Exception startupFailure;
    private boolean securityHintLogged;

    public RetreeverBootstrap(
            RetreeverOrchestrator orchestrator,
            RetreeverAuthenticationService authenticationService,
            RetreeverSecurityHintProperties securityHintProperties,
            RetreeverBasePathResolver basePathResolver) {
        this.orchestrator = orchestrator;
        this.uiLocationResolver = new RetreeverUiLocationResolver(basePathResolver);
        this.authenticationService = authenticationService;
        this.securityHintProperties = securityHintProperties;
    }

    /**
     * Triggered on {@link ApplicationReadyEvent}. Discovers controllers,
     * builds the API document using the Retreever pipeline, and caches it.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        try {
            initialize(event);
        } catch (Exception ex) {
            this.cached = null;
            this.startupFailure = ex;
            log.error(
                    "Retreever failed during startup. The host application will continue running, but Retreever endpoints may be unavailable.",
                    ex
            );
        }
    }

    private void initialize(ApplicationReadyEvent event) {
        log.debug("Initializing Retreever. Resolving API documentation.");

        ApplicationContext context = event.getApplicationContext();
        Class<?> appClass = resolveApplicationClass(context, event);

        // Scan for controllers and advices that produce response-body documentation
        Set<Class<?>> allControllers = ControllerScanner.scanControllers(context);
        Set<Class<?>> allAdvices = ControllerScanner.scanControllerAdvices(context);

        // Get base packages
        List<String> basePackages = orchestrator.getBasePackages();

        // Filter to base packages only
        Set<Class<?>> controllers = filterByBasePackages(allControllers, basePackages);
        Set<Class<?>> controllerAdvices = filterByBasePackages(allAdvices, basePackages);

        // Build final documentation snapshot
        this.cached = orchestrator.build(appClass, controllers, controllerAdvices);

        logSpringSecurityHintIfNeeded(context);

        log.info("Retreever initialized. Explore APIs at {}", uiLocationResolver.resolve(context));
    }

    /**
     * Returns the cached API document.
     */
    public ApiDocument getDocument() {
        return cached;
    }

    public boolean isAvailable() {
        return cached != null;
    }

    public Exception getStartupFailure() {
        return startupFailure;
    }

    /**
     * Returns the timestamp when the API document was built.
     */
    public Instant getUptime() {
        return cached == null ? null : cached.upTime();
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

    private Class<?> resolveApplicationClass(ApplicationContext context, ApplicationReadyEvent event) {
        Class<?> appClass = event.getSpringApplication().getMainApplicationClass();
        if (appClass != null) {
            return appClass;
        }

        String[] appBeans = context.getBeanNamesForAnnotation(SpringBootApplication.class);
        if (appBeans.length > 0) {
            Class<?> resolvedClass = context.getType(appBeans[0]);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }

        log.warn("Unable to resolve @SpringBootApplication class. Falling back to RetreeverBootstrap metadata.");
        return RetreeverBootstrap.class;
    }

    private void logSpringSecurityHintIfNeeded(ApplicationContext context) {
        if (securityHintLogged || !securityHintProperties.isHintLog() || !isRetreeverAuthOrStudioEnabled()) {
            return;
        }

        if (!hasSpringSecurity(context)) {
            return;
        }

        securityHintLogged = true;
        log.warn("""
                
                **********************************************************************
                
                    HINT:
                    Retreever detected that Spring Security is enabled in this application.
                
                    If Retreever resources are not explicitly allowed in your SecurityFilterChain,
                    the Retreever Studio and its static resources may fail with 401/403 errors.
                
                    Please allow Retreever public paths in your application security config:
                
                        @Bean
                        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                            http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                    .requestMatchers(RetreeverPublicPaths.get()).permitAll()
                                    .anyRequest().authenticated()
                                );
                
                            return http.build();
                        }
                
                    Note:
                    Retreever public paths must be accessible by the host application security layer.
                    The Retreever Studio itself can still be protected using Retreever's internal
                    authentication.
                
                    Example:
                
                        retreever.auth.username=${YOUR_USERNAME}
                        retreever.auth.password=${YOUR_PASSWORD}
                        retreever.auth.secret=${YOUR_UUID_SECRET}
                
                    To silence this hint, set:
                
                        retreever.security.hint-log=false
                
                **********************************************************************
                """);
    }

    private boolean isRetreeverAuthOrStudioEnabled() {
        return authenticationService.isEnabled() || cached != null;
    }

    private boolean hasSpringSecurity(ApplicationContext context) {
        return context.containsBean("springSecurityFilterChain") || hasSpringSecurityConfigurationAnnotation(context);
    }

    private boolean hasSpringSecurityConfigurationAnnotation(ApplicationContext context) {
        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> beanType = context.getType(beanName);
            if (beanType != null && hasSpringSecurityAnnotation(beanType)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSpringSecurityAnnotation(Class<?> beanType) {
        if ("SecurityConfig".equals(beanType.getSimpleName())) {
            return true;
        }

        for (java.lang.annotation.Annotation annotation : beanType.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if ("org.springframework.security.config.annotation.web.configuration.EnableWebSecurity".equals(annotationName)
                    || "org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity"
                    .equals(annotationName)
                    || "SecurityConfig".equals(annotation.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
