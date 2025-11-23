/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.view.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Top-level DTO representing the fully assembled Retreever API document.
 * This structure is serialized and returned from `/retreever-tool`.
 */
public record ApiDocument(
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("version") String version,
        @JsonProperty("uri_prefix") String uriPrefix,
        @JsonProperty("up_time") Instant upTime,
        @JsonProperty("groups") List<ApiGroup> groups
) {

    /**
     * Represents a controller-level API group and its endpoints.
     */
    public record ApiGroup(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("deprecated") boolean deprecated,
            @JsonProperty("endpoints") List<Endpoint> endpoints
    ) {}

    /**
     * Represents a single documented API endpoint and all associated metadata.
     */
    public record Endpoint(
            @JsonProperty("name") String name,
            @JsonProperty("deprecated") boolean deprecated,
            @JsonProperty("description") String description,
            @JsonProperty("secured") boolean secured,
            @JsonProperty("method") String method,
            @JsonProperty("path") String path,
            @JsonProperty("status") String status,
            @JsonProperty("status_code") int statusCode,
            @JsonProperty("consumes") List<String> consumes,
            @JsonProperty("produces") List<String> produces,

            @JsonProperty("path_variables") List<PathVariable> pathVariables,
            @JsonProperty("query_params") List<Param> queryParams,
            @JsonProperty("headers") List<Header> headers,

            @JsonProperty("request") Map<String, Object> request,
            @JsonProperty("response") Map<String, Object> response,

            @JsonProperty("errors") List<Error> errors
    ) {}

    /**
     * Represents a structured error entry mapped from an exception handler.
     */
    public record Error(
            @JsonProperty("status") String status,
            @JsonProperty("status_code") int statusCode,
            @JsonProperty("description") String description,
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("response") Map<String, Object> response
    ) {}

    /**
     * Represents a query parameter and its metadata.
     */
    public record Param(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("type") String type,
            @JsonProperty("required") String required,
            @JsonProperty("default_value") String defaultValue,
            @JsonProperty("constraints") List<String> constraints
    ) {}

    /**
     * Represents an HTTP header mapped from a controller method.
     */
    public record Header(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("required") String required,
            @JsonProperty("description") String description
    ) {}

    /**
     * Represents a path variable used in the endpoint URL.
     */
    public record PathVariable(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("constraints") List<String> constraints
    ) {}
}
