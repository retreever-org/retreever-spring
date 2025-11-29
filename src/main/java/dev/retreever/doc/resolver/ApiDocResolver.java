/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.doc.resolver;

import dev.retreever.annotation.ApiDoc;
import org.springframework.web.bind.annotation.RestController;
import dev.retreever.endpoint.model.ApiGroup;
import dev.retreever.group.resolver.ApiGroupResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the top-level {@link dev.retreever.endpoint.model.ApiDoc} model for the application.
 * Aggregates metadata from the main application class and resolves all
 * controller groups discovered in the codebase.
 */
public class ApiDocResolver {

    private final ApiGroupResolver groupResolver;

    public ApiDocResolver(ApiGroupResolver groupResolver) {
        this.groupResolver = groupResolver;
    }

    /**
     * Produces a fully populated {@link dev.retreever.endpoint.model.ApiDoc} containing:
     * <ul>
     *     <li>Application metadata (name, description, version)</li>
     *     <li>All resolved {@link ApiGroup} entries</li>
     * </ul>
     *
     * @param applicationClass the @SpringBootApplication class
     * @param controllers      all detected controller classes
     * @return assembled ApiDoc object
     */
    public dev.retreever.endpoint.model.ApiDoc resolve(Class<?> applicationClass, Set<Class<?>> controllers) {

        dev.retreever.endpoint.model.ApiDoc doc = new dev.retreever.endpoint.model.ApiDoc();

        resolveAppMetadata(doc, applicationClass);
        resolveControllerGroups(doc, controllers);

        return doc;
    }

    /**
     * Loads application-level metadata from {@code @ApiDoc} or
     * derives sensible defaults when absent.
     */
    private void resolveAppMetadata(dev.retreever.endpoint.model.ApiDoc doc, Class<?> applicationClass) {

        ApiDoc ann =
                applicationClass.getAnnotation(ApiDoc.class);

        if (ann != null) {
            doc.setName(ann.name());
            doc.setDescription(ann.description());
            doc.setVersion(ann.version());
        } else {
            // fallback to prettified class name
            doc.setName(prettifyName(applicationClass.getSimpleName()));
            doc.setDescription("");
            doc.setVersion("v1");
        }
    }

    /**
     * Converts all {@code @RestController} classes into {@link ApiGroup} entries.
     * Only groups with at least one endpoint are included.
     */
    private void resolveControllerGroups(dev.retreever.endpoint.model.ApiDoc doc, Set<Class<?>> controllers) {

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
     * Creates a user-friendly name from the application class name.
     * Example: "RetreeverApplication" → "Retreever"
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
