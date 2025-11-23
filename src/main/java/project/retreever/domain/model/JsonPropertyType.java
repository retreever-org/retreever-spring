/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.domain.model;

/**
 * Supported JSON value types used when building schema properties.
 */
public enum JsonPropertyType {
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    OBJECT,
    ARRAY,
    ENUM
}
