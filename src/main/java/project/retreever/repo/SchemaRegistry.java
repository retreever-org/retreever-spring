package project.retreever.repo;

import project.retreever.domain.model.JsonProperty;
import project.retreever.schema.resolver.JsonSchemaResolver;
import project.retreever.schema.resolver.TypeResolver;

import java.util.List;

public class SchemaRegistry extends DocRegistry<List<JsonProperty>> {

    private final JsonSchemaResolver jsonSchemaResolver;

    public SchemaRegistry(JsonSchemaResolver jsonSchemaResolver) {
        this.jsonSchemaResolver = jsonSchemaResolver;
    }

    public String registerSchema(Class<?> dtoClass) {
        String ref = TypeResolver.resolveRefName(dtoClass);

        if (!contains(ref)) {
            List<JsonProperty> props = jsonSchemaResolver.resolve(dtoClass);
            add(ref, props);
        }

        return ref;
    }
}

