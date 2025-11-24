/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.view;

import dev.retreever.domain.model.JsonProperty;

import java.util.*;

/**
 * Converts a resolved tree of {@link JsonProperty} objects into a
 * renderable schema block containing:
 *
 * <ul>
 *     <li><b>model</b> – type-only structure</li>
 *     <li><b>example_model</b> – example values</li>
 *     <li><b>metadata</b> – constraints + description in dot-notation paths</li>
 * </ul>
 */
public class ApiSchemaRenderer {

    public enum SchemaType {
        RESPONSE, REQUEST
    }

    /**
     * Builds a composite block containing:
     * model, example_model, metadata.
     */
    public static Map<String, Object> execute(List<JsonProperty> properties, SchemaType schemaType) {

        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> example = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();

        for (JsonProperty p : properties) {
            buildModel(p, model);
            buildExample(p, example);
            if (schemaType.equals(SchemaType.REQUEST)) {
                buildMetadata(p, "", metadata);
            }
        }

        root.put("model", model);
        root.put("example_model", example);

        if (schemaType.equals(SchemaType.REQUEST)) {
            root.put("metadata", metadata);
        }

        return root;
    }

    // ----------------------------------------------------------------------
    // MODEL RENDERING
    // ----------------------------------------------------------------------

    private static void buildModel(JsonProperty prop, Map<String, Object> out) {

        switch (prop.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL,
                 UUID, DATE, TIME, DATE_TIME -> {
                out.put(prop.getName(), jsonType(prop));
            }

            case OBJECT -> {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (JsonProperty child : prop.getProperties()) {
                    buildModel(child, nested);
                }
                out.put(prop.getName(), nested);
            }

            case ARRAY -> {
                List<Object> arr = new ArrayList<>();
                JsonProperty el = prop.getArrayElement();
                if (el != null) arr.add(renderModelArrayElement(el));
                out.put(prop.getName(), arr);
            }
        }
    }

    private static Object renderModelArrayElement(JsonProperty el) {

        return switch (el.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL,
                 UUID, DATE, TIME, DATE_TIME -> jsonType(el);

            case OBJECT -> {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (JsonProperty c : el.getProperties()) {
                    buildModel(c, nested);
                }
                yield nested;
            }

            case ARRAY -> {
                List<Object> list = new ArrayList<>();
                JsonProperty inner = el.getArrayElement();
                if (inner != null) list.add(renderModelArrayElement(inner));
                yield list;
            }

            case URI -> jsonType(el);   // string

            case DURATION -> jsonType(el);   // string

            case PERIOD -> jsonType(el);   // string

            case BINARY -> jsonType(el);   // string (base64)
        };
    }


    private static String jsonType(JsonProperty p) {
        return p.getType().displayName();
    }

    // EXAMPLE RENDERING

    private static void buildExample(JsonProperty prop, Map<String, Object> out) {

        switch (prop.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL,
                 UUID, DATE, TIME, DATE_TIME -> {
                out.put(prop.getName(), prop.getExampleValue());
            }

            case OBJECT -> {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (JsonProperty c : prop.getProperties()) {
                    buildExample(c, nested);
                }
                out.put(prop.getName(), nested);
            }

            case ARRAY -> {
                List<Object> arr = new ArrayList<>();
                JsonProperty el = prop.getArrayElement();
                if (el != null) arr.add(renderExampleArrayElement(el));
                out.put(prop.getName(), arr);
            }
        }
    }

    private static Object renderExampleArrayElement(JsonProperty el) {

        return switch (el.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL,
                 UUID, DATE, TIME, DATE_TIME -> el.getExampleValue();

            case OBJECT -> {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (JsonProperty c : el.getProperties()) {
                    buildExample(c, nested);
                }
                yield nested;
            }

            case ARRAY -> {
                List<Object> nested = new ArrayList<>();
                JsonProperty inner = el.getArrayElement();
                if (inner != null) nested.add(renderExampleArrayElement(inner));
                yield nested;
            }

            case URI -> {
                Object ex = el.getExampleValue();
                yield (ex != null) ? ex : "https://example.com";
            }

            case DURATION -> {
                Object ex = el.getExampleValue();
                yield (ex != null) ? ex : "PT0S";   // ISO-8601 zero duration
            }

            case PERIOD -> {
                Object ex = el.getExampleValue();
                yield (ex != null) ? ex : "P0D";    // ISO-8601 zero period
            }

            case BINARY -> {
                // No meaningful default — returning null is correct
                yield null;
            }
        };
    }

    // METADATA RENDERING

    private static void buildMetadata(
            JsonProperty prop,
            String parentPath,
            Map<String, Object> out
    ) {

        String key = parentPath.isEmpty()
                ? prop.getName()
                : parentPath + "." + prop.getName();

        switch (prop.getType()) {

            case OBJECT -> {
                for (JsonProperty child : prop.getProperties()) {
                    buildMetadata(child, key, out);
                }
            }

            case ARRAY -> {
                JsonProperty el = prop.getArrayElement();
                if (el != null) buildMetadata(el, key, out);
            }

            case STRING, NUMBER, BOOLEAN, ENUM, NULL,
                 UUID, DATE, TIME, DATE_TIME -> {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("description",
                        Optional.ofNullable(prop.getDescription()).orElse(""));
                meta.put("constraints",
                        new ArrayList<>(prop.getConstraints()));
                out.put(key, meta);
            }
        }
    }
}
