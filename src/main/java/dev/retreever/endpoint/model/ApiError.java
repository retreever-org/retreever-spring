package dev.retreever.endpoint.model;

import org.springframework.http.HttpStatus;

import java.lang.reflect.Type;

/**
 * Represents a resolved error model for an API endpoint.
 * Built from @ExceptionHandler methods.
 * <p>
 * Stores:
 * - HTTP status
 * - description
 * - fully qualified exception name (reference key)
 * - Type of the error body (resolved later by SchemaResolver)
 * </p>
 */
public class ApiError {

    private final HttpStatus status;
    private final String description;
    private final String exceptionName; // fully qualified exception type
    private String errorCode;

    private Type errorBodyType; // resolved at view-assembly stage

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

    public ApiError setErrorBodyType(Type type) {
        this.errorBodyType = type;
        return this;
    }

    // ───────────── Getters ─────────────

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

    public Type getErrorBodyType() {
        return errorBodyType;
    }
}
