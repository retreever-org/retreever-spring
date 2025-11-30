/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.engine;

import dev.retreever.config.SchemaConfig;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.model.Schema;
import dev.retreever.schema.resolver.SchemaResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates complete schema resolution for REST controllers and exception handlers.
 * Stores UNWRAPPED schemas with TRUE wrapped types as keys (ResponseEntity<T>, etc.)
 */
public class SchemaResolutionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SchemaResolutionOrchestrator.class);

    private final SchemaRegistry schemaRegistry;

    public SchemaResolutionOrchestrator(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
        log.debug("packages allowed for scanning: {}", SchemaConfig.getBasePackages());
    }

    public void resolveAllSchema(Class<?> applicationClass,
                                 Set<Class<?>> controllers,
                                 Set<Class<?>> controllerAdvices) {

        // Process REST Controllers
        processControllers(controllers);

        // Process Exception Handlers
        processControllerAdvices(controllerAdvices);

        log.debug("All Schema Successfully Resolved.");
        schemaRegistry.getSchemas().values().forEach(schema -> {
                    log.debug(schema.toString());
                }
        );

        log.debug("SchemaResolutionOrchestrator: {} schemas registered", schemaRegistry.size());
    }

    private void processControllers(Set<Class<?>> controllers) {
        for (Class<?> controller : controllers) {
            if (isBasePackageClass(controller)) continue;

            for (Method method : controller.getDeclaredMethods()) {
                if (!isRestEndpoint(method)) continue;

                log.debug("Processing endpoint: {}", method.getName());

                // 1. REGISTER RAW RETURN TYPE with its unwrapped schema
                processReturnType(method.getGenericReturnType());

                // 2. REGISTER @RequestBody/@ModelAttribute schemas
                processMethodParameters(method);
            }
        }
    }

    private void processControllerAdvices(Set<Class<?>> controllerAdvices) {
        for (Class<?> advice : controllerAdvices) {
            if (isBasePackageClass(advice)) continue;

            for (Method method : advice.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(ExceptionHandler.class)) continue;

                // Register exception handler return type
                processReturnType(method.getGenericReturnType());

                // Register request body parameters (if any)
                processMethodParameters(method);
            }
        }
    }

    /**
     * CORE LOGIC: Store UNWRAPPED schema with TRUE wrapped type as key
     * ResponseEntity<ApiResponse<ProductResponse>> → schema of ApiResponse<ProductResponse>
     */
    private void processReturnType(Type rawReturnType) {
        if (rawReturnType == null || isVoid(rawReturnType)) return;

        log.debug("Return type: {}", rawReturnType.getTypeName());

        // KEY = TRUE return type (ResponseEntity<T>)
        // VALUE = Schema of unwrapped T
        Type unwrappedType = unwrapContainerType(rawReturnType);
        registerSchema(rawReturnType, unwrappedType);
    }

    private void processMethodParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            if (isRequestBodyOrModelAttribute(param)) {
                Type rawParamType = param.getParameterizedType();
                Type unwrappedType = unwrapContainerType(rawParamType);
                registerSchema(rawParamType, unwrappedType);
            }
        }
    }

    /**
     * PERFECT MATCH: Key=WrappedType, Schema=UnwrappedType
     */
    private void registerSchema(Type keyType, Type unwrappedType) {
        Class<?> rawClass = SchemaResolver.extractRawClass(unwrappedType);
        if (rawClass == null || rawClass.isPrimitive() || rawClass.isEnum() || isBasePackageClass(rawClass)) {
            return;
        }

        Schema schema = SchemaResolver.initResolution(unwrappedType);
        schemaRegistry.register(keyType, schema);
        log.debug("Registered: {} → {}", keyType.getTypeName(), schema.getClass().getSimpleName());
    }

    // === TYPE UNWRAPPING ===

    private Type unwrapContainerType(Type type) {
        Class<?> rawType = SchemaResolver.extractRawClass(type);

        // ResponseEntity<T> → T
        if (rawType == ResponseEntity.class && type instanceof ParameterizedType pt) {
            return pt.getActualTypeArguments()[0];
        }

        // Optional<T> → T
        if (rawType == Optional.class && type instanceof ParameterizedType pt) {
            return pt.getActualTypeArguments()[0];
        }

        // Page<T> → T
        if ("org.springframework.data.domain.Page".equals(rawType.getName()) && type instanceof ParameterizedType pt) {
            return pt.getActualTypeArguments()[0];
        }

        return type; // Simple types
    }

    private boolean isVoid(Type type) {
        Class<?> rawClass = SchemaResolver.extractRawClass(type);
        return rawClass == void.class || rawClass == Void.class;
    }

    // === FILTERING ===

    private boolean isBasePackageClass(Class<?> clazz) {
        if (clazz == null) return true;
        String packageName = clazz.getPackageName();
        return SchemaConfig.getBasePackages().stream()
                .noneMatch(packageName::startsWith);
    }

    // === ENDPOINT DETECTION ===

    private boolean isRestEndpoint(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
                method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class);
    }

    private boolean isRequestBodyOrModelAttribute(Parameter param) {
        return param.isAnnotationPresent(RequestBody.class) ||
                param.isAnnotationPresent(ModelAttribute.class);
    }
}
