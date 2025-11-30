/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.view;

import dev.retreever.schema.model.*;

import java.util.*;

/**
 * Renders Schema → 3-part JSON structure for API documentation (model, example, metadata).
 */
public final class SchemaViewRenderer {

    public static final String MODEL_KEY = "model";
    public static final String EXAMPLE_MODEL_KEY = "example_model";
    public static final String METADATA_KEY = "metadata";
    public static final String DESCRIPTION = "description";
    public static final String CONSTRAINTS = "constraints";
    public static final String REQUIRED = "required";

    private SchemaViewRenderer() {}

    public static Map<String, Object> renderRequest(Schema schema) {
        return render(schema, true);
    }

    public static Map<String, Object> renderResponse(Schema schema) {
        return render(schema, false);
    }

    private static Map<String, Object> render(Schema schema, boolean includeMetadata) {
        if (schema == null) return new LinkedHashMap<>();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(MODEL_KEY, renderModel(schema));
        result.put(EXAMPLE_MODEL_KEY, renderExample(schema));

        if (includeMetadata) {
            result.put(METADATA_KEY, buildMetadata(schema));
        }
        return result;
    }

    private static Object renderModel(Schema s) {
        if (s == null) return null;
        if (s instanceof Property p) return renderModel(p.getValue());
        if (s instanceof ValueSchema vs) return vs.getType().displayName();
        if (s instanceof ArraySchema arr) {
            Schema element = arr.getElementSchema();
            Object model = renderModel(element);
            return model != null ? List.of(model) : new ArrayList<>();
        }
        if (s instanceof ObjectSchema obj) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Property p : obj.getProperties().values()) {
                out.put(p.getName(), renderModel(p));
            }
            return out;
        }
        return null;
    }

    private static Object renderExample(Schema s) {
        if (s == null) return null;

        if (s instanceof Property p) {
            if (p.getExample() != null) {
                return p.getExample();
            }
            return renderExample(p.getValue());
        }

        if (s instanceof ArraySchema arr) {
            Schema element = arr.getElementSchema();
            Object example = renderExample(element);
            return example != null ? List.of(example) : new ArrayList<>();
        }

        if (s instanceof ObjectSchema obj) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Property p : obj.getProperties().values()) {
                out.put(p.getName(), renderExample(p));
            }
            return out;
        }

        return generateLeafExample(s);
    }

    private static Object generateLeafExample(Schema s) {
        if (s instanceof ValueSchema vs) {
            return switch (vs.getType()) {
                case STRING -> "hello";
                case NUMBER -> 123;
                case BOOLEAN -> true;
                case UUID -> "550e8400-e29b-41d4-a716-446655440000";
                case DATE_TIME -> "2025-01-29T10:15:30Z";
                case DATE -> "2025-01-29";
                default -> null;
            };
        }
        return null;
    }

    private static Map<String, Object> buildMetadata(Schema schema) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        buildMetadata(schema, "", metadata);
        return metadata;
    }

    private static void buildMetadata(Schema s, String path, Map<String, Object> out) {
        if (s == null) return;

        if (s instanceof Property p) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put(DESCRIPTION, p.getDescription() != null ? p.getDescription() : "");
            meta.put(REQUIRED, p.isRequired());
            meta.put(CONSTRAINTS, new ArrayList<>(p.getConstraints()));
            out.put(path.isEmpty() ? p.getName() : path, meta);
            return;
        }

        if (s instanceof ArraySchema arr) {
            Schema el = arr.getElementSchema();
            if (el != null) buildMetadata(el, path + "[0]", out);
            return;
        }

        if (s instanceof ObjectSchema obj) {
            for (Property p : obj.getProperties().values()) {
                String newPath = path.isEmpty() ? p.getName() : path + "." + p.getName();
                buildMetadata(p, newPath, out);
            }
        }
    }
}
