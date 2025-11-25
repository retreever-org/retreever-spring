package dev.retreever.schema.resolver;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public class GenericContext {

    private final Map<TypeVariable<?>, Type> bindings = new HashMap<>();

    public void bind(Type type) {
        if (!(type instanceof ParameterizedType p)) return;

        var raw = p.getRawType();
        if (!(raw instanceof Class<?> clazz)) return;

        var vars = clazz.getTypeParameters();
        var args = p.getActualTypeArguments();

        for (int i = 0; i < vars.length; i++) {
            bindings.put(vars[i], args[i]);
        }
    }

    public Type resolve(Type type) {
        if (type instanceof TypeVariable<?> var) {
            return bindings.getOrDefault(var, Object.class);
        }
        return type;
    }
}
