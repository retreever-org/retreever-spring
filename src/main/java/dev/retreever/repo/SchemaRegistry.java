/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     [https://opensource.org/licenses/MIT](https://opensource.org/licenses/MIT)
 */

package dev.retreever.repo;

import dev.retreever.schema.model.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe singleton registry for resolved schemas using Type.getTypeName() keys.
 * Type-only registration for maximum safety and simplicity.
 */
public final class SchemaRegistry {
    Logger log = LoggerFactory.getLogger(SchemaRegistry.class);
    private static final SchemaRegistry INSTANCE = new SchemaRegistry();
    private static final Map<String, Schema> schemas = new ConcurrentHashMap<>();

    private SchemaRegistry() {}

    public static SchemaRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers schema for the given type using Type.getTypeName(). Deduplicates automatically.
     */
    public void register(Type type, Schema schema) {
        if (type == null || schema == null) return;

        String typeName = type.getTypeName();
        if (typeName != null) {
            schemas.putIfAbsent(typeName, schema);
        }
    }

    /**
     * Retrieves schema by type.
     */
    public Schema getSchema(Type type) {
        if (type == null) return null;
        String typeName = type.getTypeName();
        return typeName != null ? schemas.get(typeName) : null;
    }

    /**
     * Optimizes registry: log stats.
     */
    public void optimize() {
        log.info("SchemaRegistry: {} unique schemas registered", schemas.size());
    }

    public int size() {
        return schemas.size();
    }

    public void clear() {
        schemas.clear();
    }

    public Map<String, Schema> getSchemas() {
        return schemas;
    }
}
