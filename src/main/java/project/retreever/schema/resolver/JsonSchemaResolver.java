package project.retreever.schema.resolver;

import project.retreever.domain.model.JsonProperty;
import project.retreever.domain.model.JsonPropertyType;

import java.lang.reflect.*;
import java.util.*;

/**
 * Resolves JSON Schema-like properties recursively from Java classes.
 * <p>
 * This resolver supports cyclic references detection,
 * differentiates property types (object, array, primitives),
 * and delegates detailed processing to specialized handlers.
 * </p>
 * <p>
 * Usage flow:
 * <ul>
 * <li>Call {@link #resolve(Class)} with the top-level class.</li>
 * <li>Internally, each field is inspected via {@link #resolveField(Field)}.</li>
 * <li>Depending on type, delegates to {@link #handleObject(JsonProperty, Class)},
 * {@link #handleArray(JsonProperty, Type)} or {@link #handlePrimitive(JsonProperty, Field)}.</li>
 * <li>Specialized resolvers are invoked for constraints, description, and example metadata on primitives.</li>
 * </ul>
 * </p>
 */
public class JsonSchemaResolver {

    /**
     * Tracks classes currently being processed to prevent infinite recursion on cyclic references.
     */
    private final Set<Class<?>> visited = new HashSet<>();

    /**
     * Resolves the JSON schema properties for the given class.
     *
     * @param clazz Java class to resolve schema properties for
     * @return list of {@link JsonProperty} representing the schema, empty if null class or cyclic reference
     */
    public List<JsonProperty> resolve(Class<?> clazz) {
        if (clazz == null || visited.contains(clazz)) {
            return List.of();
        }

        visited.add(clazz);

        List<JsonProperty> schema = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            schema.add(resolveField(field));
        }

        visited.remove(clazz);
        return schema;
    }

    /**
     * Resolves a single field to a {@link JsonProperty}, delegating handling based on property type.
     *
     * @param field Java reflection {@link Field} to resolve
     * @return resolved {@link JsonProperty}
     */
    private JsonProperty resolveField(Field field) {
        Type type = field.getGenericType();
        Class<?> rawClass = TypeResolver.extractRawClass(type);

        JsonPropertyType propType = JsonPropertyTypeResolver.resolve(rawClass);
        JsonProperty jsonProp = JsonProperty.of(field.getName(), propType);

        switch (propType) {
            case OBJECT -> handleObject(jsonProp, rawClass);
            case ARRAY -> handleArray(jsonProp, type);
            default -> handlePrimitive(jsonProp, field);
        }

        return jsonProp;
    }

    /**
     * Handles resolving nested object properties recursively.
     *
     * @param jsonProp the {@link JsonProperty} to attach nested properties to
     * @param rawClass the class type of the nested object
     */
    private void handleObject(JsonProperty jsonProp, Class<?> rawClass) {
        List<JsonProperty> nested = resolve(rawClass);
        nested.forEach(jsonProp::addProperty);
    }

    /**
     * Handles resolving elements of array or collection properties recursively.
     *
     * @param jsonProp Java {@link JsonProperty} representing the array
     * @param fieldType generic type (including parameterization) of the array field
     */
    private void handleArray(JsonProperty jsonProp, Type fieldType) {
        Type elemType = extractElementType(fieldType);
        if (elemType == null) return;

        Class<?> elemClass = TypeResolver.extractRawClass(elemType);
        List<JsonProperty> elementSchema = resolve(elemClass);

        if (!elementSchema.isEmpty()) {
            jsonProp.arrayElement(elementSchema.get(0)); // attach first property as example element schema
        }
    }

    /**
     * Handles primitive, string, enum, boolean, and null types by applying constraints,
     * descriptions, and example values.
     *
     * @param jsonProp the {@link JsonProperty} to enrich
     * @param field reflection {@link Field} for annotation extraction
     */
    private void handlePrimitive(JsonProperty jsonProp, Field field) {
        JsonPropertyConstraintResolver.resolve(jsonProp, field);
        JsonPropertyDescriptionResolver.resolve(jsonProp, field);
        JsonPropertyExampleResolver.resolve(jsonProp, field);
    }

    /**
     * Extracts the element type parameter from an array or generic collection type.
     *
     * @param type a Java {@link Type} representing array or generic collection
     * @return element {@link Type} if detected, otherwise {@code null}
     */
    private Type extractElementType(Type type) {
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            return clazz.getComponentType();
        }
        return TypeResolver.getTypeParameter(type);
    }
}
