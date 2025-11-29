package dev.retreever.schema.context;

import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.resolver.SchemaResolver;

public record ResolverContext(
        PropertyTypeContext typeContext,
        SchemaRegistry registry,
        SchemaResolver orchestrator
) {}
