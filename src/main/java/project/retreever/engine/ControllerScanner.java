/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.engine;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans the Spring application context for all {@link RestController}-annotated
 * classes by inspecting registered request handler mappings.
 */
public class ControllerScanner {

    /**
     * Returns all controller classes detected by Spring MVC's
     * {@link RequestMappingHandlerMapping}.
     *
     * @param context the active Spring application context
     * @return a set of controller types
     */
    public static Set<Class<?>> scanControllers(ApplicationContext context) {

        RequestMappingHandlerMapping mapping =
                context.getBean(RequestMappingHandlerMapping.class);

        return mapping.getHandlerMethods()
                .values()
                .stream()
                .map(HandlerMethod::getBeanType)
                .filter(type -> type.isAnnotationPresent(RestController.class))
                .collect(Collectors.toSet());
    }
}
