package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.domain.model.JsonPropertyType;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class JsonPropertyResolver {

    public static JsonProperty resolve(Field field, Type actualType, GenericContext context) {

        Type unfolded = context.resolve(actualType);
        Class<?> rawClass = TypeResolver.extractRawClass(unfolded);

        JsonPropertyType propType = JsonPropertyTypeResolver.resolve(rawClass);

        JsonProperty prop = JsonProperty.of(field.getName(), propType);

        switch (propType) {

            case OBJECT -> {
                JsonObjectResolver resolver = new JsonObjectResolver(context);
                prop.addObjectProperty(resolver.resolve(unfolded));
                return prop;
            }

            case ARRAY -> {
                JsonArrayResolver arr = new JsonArrayResolver(context);
                prop.arrayElement(arr.resolve(unfolded));
                return prop;
            }

            default -> {
                JsonPropertyConstraintResolver.resolve(prop, field);
                JsonPropertyDescriptionResolver.resolve(prop, field);
                JsonPropertyExampleResolver.resolve(prop, field);
                return prop;
            }
        }
    }
}
