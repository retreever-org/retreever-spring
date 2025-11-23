/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.domain.model;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a resolved error model for an API endpoint.
 * Built from {@code @ExceptionHandler} methods and includes
 * status, description, exception type, and an optional error body schema.
 */
public class ApiError {

    private final HttpStatus status;
    private final String description;
    private final String exceptionName;       // fully qualified exception type
    private String errorCode;                 // optional custom code

    private final List<JsonProperty> errorBody = new ArrayList<>();

    private ApiError(HttpStatus status, String description, String exceptionName) {
        this.status = status;
        this.description = description;
        this.exceptionName = exceptionName;
    }

    public static ApiError create(HttpStatus status, String description, String exceptionName) {
        return new ApiError(status, description, exceptionName);
    }

    public ApiError errorCode(String code) {
        this.errorCode = code;
        return this;
    }

    public ApiError addErrorProperty(JsonProperty property) {
        this.errorBody.add(property);
        return this;
    }

    public ApiError setErrorBody(List<JsonProperty> properties) {
        this.errorBody.clear();
        this.errorBody.addAll(properties);
        return this;
    }

    // ──────────────────────────────
    // Getters
    // ──────────────────────────────

    public HttpStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<JsonProperty> getErrorBody() {
        return errorBody;
    }
}
