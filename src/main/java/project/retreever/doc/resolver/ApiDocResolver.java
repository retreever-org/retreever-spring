/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.doc.resolver;

import org.springframework.web.bind.annotation.RestController;
import project.retreever.domain.model.ApiDoc;
import project.retreever.domain.model.ApiGroup;
import project.retreever.group.resolver.ApiGroupResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves the application-level ApiDoc model.
 */
public class ApiDocResolver {

    private final ApiGroupResolver groupResolver;

    public ApiDocResolver(ApiGroupResolver groupResolver) {
        this.groupResolver = groupResolver;
    }

    /**
     * Resolves metadata + all controller groups into a complete ApiDoc object.
     *
     * @param applicationClass The @SpringBootApplication class
     * @param controllers      All controller classes in the application
     */
    public ApiDoc resolve(Class<?> applicationClass, Set<Class<?>> controllers) {

        ApiDoc doc = new ApiDoc();

        resolveAppMetadata(doc, applicationClass);
        resolveControllerGroups(doc, controllers);

        return doc;
    }

    /**
     * Loads application-level metadata from @ApiDoc or defaults.
     */
    private void resolveAppMetadata(ApiDoc doc, Class<?> applicationClass) {

        project.retreever.domain.annotation.ApiDoc ann = applicationClass.getAnnotation(project.retreever.domain.annotation.ApiDoc.class);

        // name
        if (ann != null) {
            doc.setName(ann.name());
            doc.setDescription(ann.description());
            doc.setVersion(ann.version());
        } else {
            // fallback to class name
            doc.setName(prettifyName(applicationClass.getSimpleName()));
            doc.setDescription("");
            doc.setVersion("v1");
        }
    }

    /**
     * Resolves all controller classes into ApiGroups.
     */
    private void resolveControllerGroups(ApiDoc doc, Set<Class<?>> controllers) {

        List<ApiGroup> groups = new ArrayList<>();

        for (Class<?> controller : controllers) {
            if (!controller.isAnnotationPresent(RestController.class)) continue;

            ApiGroup group = groupResolver.resolve(controller);
            if (group != null && !group.getEndpoints().isEmpty()) {
                groups.add(group);
            }
        }

        doc.setGroups(groups);
    }

    /**
     * Converts "RetreeverApplication" -> "Retreever Application"
     */
    private String prettifyName(String raw) {
        String spaced = raw
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("Application$", "")
                .trim();

        return spaced.isEmpty() ? "API Documentation" : spaced;
    }
}
