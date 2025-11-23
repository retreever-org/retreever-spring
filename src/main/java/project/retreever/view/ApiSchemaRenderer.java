package project.retreever.view;

import project.retreever.domain.model.JsonProperty;

import java.util.*;

/**
 * Builds structure used by API documentation for:
 *  - model
 *  - example_model
 *  - metadata (dot-notation, constraints array)
 */
public class ApiSchemaRenderer {

    public static Map<String, Object> execute(List<JsonProperty> properties) {

        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> example = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();

        for (JsonProperty p : properties) {
            buildModel(p, model);
            buildExample(p, example);
            buildMetadata(p, "", metadata);
        }

        root.put("model", model);
        root.put("example_model", example);
        root.put("metadata", metadata);

        return root;
    }

    // =====================================================================
    // MODEL
    // =====================================================================

    private static void buildModel(JsonProperty prop, Map<String, Object> out) {

        switch (prop.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL -> {
                out.put(prop.getName(), jsonType(prop));
            }

            case OBJECT -> {
                Map<String, Object> obj = new LinkedHashMap<>();
                for (JsonProperty child : prop.getProperties()) {
                    buildModel(child, obj);
                }
                out.put(prop.getName(), obj);
            }

            case ARRAY -> {
                List<Object> list = new ArrayList<>();
                JsonProperty el = prop.getArrayElement();

                if (el != null) {
                    list.add(renderModelArrayElement(el));
                }

                out.put(prop.getName(), list);
            }
        }
    }

    private static Object renderModelArrayElement(JsonProperty el) {
        return switch (el.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM -> jsonType(el);
            case NULL -> null;

            case OBJECT -> {
                Map<String, Object> child = new LinkedHashMap<>();
                for (JsonProperty c : el.getProperties()) {
                    buildModel(c, child);
                }
                yield child;
            }

            case ARRAY -> {
                List<Object> nested = new ArrayList<>();
                JsonProperty inner = el.getArrayElement();
                if (inner != null) {
                    nested.add(renderModelArrayElement(inner));
                }
                yield nested;
            }
        };
    }

    private static String jsonType(JsonProperty p) {
        return p.getType().name().toLowerCase();
    }

    // =====================================================================
    // EXAMPLE MODEL
    // =====================================================================

    private static void buildExample(JsonProperty prop, Map<String, Object> out) {

        switch (prop.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM, NULL -> out.put(prop.getName(), prop.getExampleValue());

            case OBJECT -> {
                Map<String, Object> child = new LinkedHashMap<>();
                for (JsonProperty c : prop.getProperties()) {
                    buildExample(c, child);
                }
                out.put(prop.getName(), child);
            }

            case ARRAY -> {
                List<Object> arr = new ArrayList<>();
                JsonProperty el = prop.getArrayElement();
                if (el != null) {
                    arr.add(renderExampleArrayElement(el));
                }
                out.put(prop.getName(), arr);
            }
        }
    }

    private static Object renderExampleArrayElement(JsonProperty el) {

        return switch (el.getType()) {

            case STRING, NUMBER, BOOLEAN, ENUM -> el.getExampleValue();
            case NULL -> null;

            case OBJECT -> {
                Map<String, Object> obj = new LinkedHashMap<>();
                for (JsonProperty c : el.getProperties()) {
                    buildExample(c, obj);
                }
                yield obj;
            }

            case ARRAY -> {
                List<Object> nested = new ArrayList<>();
                JsonProperty inner = el.getArrayElement();
                if (inner != null) {
                    nested.add(renderExampleArrayElement(inner));
                }
                yield nested;
            }
        };
    }

    // =====================================================================
    // METADATA (dot notation, constraints as array)
    // =====================================================================

    private static void buildMetadata(JsonProperty prop,
                                      String parentPath,
                                      Map<String, Object> out) {

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

            case STRING, NUMBER, BOOLEAN, ENUM, NULL -> {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("description", Optional.ofNullable(prop.getDescription()).orElse(""));
                meta.put("constraints", new ArrayList<>(prop.getConstraints()));
                out.put(key, meta);
            }
        }
    }
}
