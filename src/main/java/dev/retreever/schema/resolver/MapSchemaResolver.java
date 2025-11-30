/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.schema.context.ResolverContext;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.model.MapSchema;
import dev.retreever.schema.model.Schema;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Resolves Map&lt;K,V&gt; types into MapSchema representations.
 * Extracts generic key/value types using ResolverContext.
 */
public class MapSchemaResolver {

    public static Schema resolve(Type resolvedType) {
        ResolverContext ctx = SchemaResolver.CONTEXT.get();
        Type substitutedType = ctx.substitute(resolvedType);

        // Handle raw Map (no generics) → Map<String,Object>
        if (!(substitutedType instanceof ParameterizedType pt)) {
            JsonPropertyType stringType = JsonPropertyType.STRING;
            Schema objectSchema = SchemaResolver.resolve(Object.class);
            return new MapSchema(stringType, objectSchema);
        }

        // Extract Map<K,V> generics
        Class<?> rawType = (Class<?>) pt.getRawType();
        if (!java.util.Map.class.isAssignableFrom(rawType)) {
            return SchemaResolver.resolve(Object.class);
        }

        Type[] typeArgs = pt.getActualTypeArguments();
        Type keyType = typeArgs.length > 0 ? typeArgs[0] : String.class;
        Type valueType = typeArgs.length > 1 ? typeArgs[1] : Object.class;

        // Substitute any type variables (T → concrete type)
        keyType = ctx.substitute(keyType);
        valueType = ctx.substitute(valueType);

        // Resolve key to JsonPropertyType, value to full Schema
        JsonPropertyType keyPropType = JsonPropertyTypeResolver.resolve(
                SchemaResolver.extractRawClass(keyType)
        );
        Schema valueSchema = SchemaResolver.resolve(valueType);

        return new MapSchema(keyPropType, valueSchema);
    }
}
