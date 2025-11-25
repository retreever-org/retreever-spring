/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.domain.model.JsonPropertyType;

import java.lang.reflect.*;

public class JsonArrayResolver {

    private final GenericContext context;

    public JsonArrayResolver(GenericContext context) {
        this.context = context;
    }

    public JsonProperty resolve(Type type) {

        // Map<K,V> → V
        if (TypeResolver.isMap(type)) {
            Type v = TypeResolver.getMapValueType(type);
            return resolveElement(v, type);
        }

        // Collection<T>
        Type elem = extractElement(type);
        if (elem != null) {
            return resolveElement(elem, type);
        }

        // Array T[]
        if (type instanceof Class<?> c && c.isArray()) {
            return resolveElement(c.getComponentType(), type);
        }

        return JsonProperty.of("element", JsonPropertyType.OBJECT);
    }

    private JsonProperty resolveElement(Type elementType, Type parentType) {

        Type unfolded = context.resolve(elementType);
        Class<?> raw = TypeResolver.extractRawClass(unfolded);

        if (raw == null) {
            return JsonProperty.of("element", JsonPropertyType.OBJECT);
        }

        JsonPropertyType type = JsonPropertyTypeResolver.resolve(raw);
        JsonProperty elem = JsonProperty.of(raw.getSimpleName(), type);

        // --- SELF-RECURSION CHECK (Case 1) ---
        // If List<A> is being resolved while resolving A → stop
        if (parentType instanceof ParameterizedType p) {
            Type parentElem = p.getActualTypeArguments()[0];
            if (TypeResolver.extractRawClass(parentElem) == raw) {
                return JsonProperty.reference(raw.getSimpleName());
            }
        }

        // Same logic for arrays
        if (parentType instanceof Class<?> pc && pc.isArray()) {
            if (pc.getComponentType() == raw) {
                return JsonProperty.reference(raw.getSimpleName());
            }
        }

        switch (type) {

            case OBJECT -> {
                JsonObjectResolver obj = new JsonObjectResolver(context);
                elem.addObjectProperty(obj.resolve(unfolded));
                return elem;
            }

            case ARRAY -> {
                Type inner = extractElement(unfolded);
                elem.arrayElement(resolveElement(inner, elementType));
                return elem;
            }

            default -> {
                return elem;
            }
        }
    }

    private Type extractElement(Type type) {

        if (type instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            if (args.length > 0) {
                return args[0];
            }
        }

        if (type instanceof Class<?> c && c.isArray()) {
            return c.getComponentType();
        }

        return null;
    }
}
