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

public class ControllerScanner {

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
