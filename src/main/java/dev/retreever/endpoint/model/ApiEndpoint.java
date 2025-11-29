package dev.retreever.endpoint.model;

import org.springframework.http.HttpStatus;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a fully resolved API endpoint, including path info,
 * HTTP metadata, parameters, schemas, and error models.
 * <p>
 * Instead of storing string schema references, this version stores
 * raw Java {@link Type} objects so the SchemaResolver can correctly
 * handle generic types such as:
 * <p>
 * ApiResponse<User>
 * ApiResponse<List<User>>
 * List<ApiResponse<Product>>
 * <p>
 * Type fidelity is now preserved, ensuring correct schema generation.
 */
public class ApiEndpoint {

    private String name;
    private String path;
    private String httpMethod;
    private HttpStatus status;
    private String description;
    private boolean isSecured = false;

    private List<String> consumes;
    private List<String> produces;

    private List<ApiPathVariable> pathVariables;
    private List<ApiParam> queryParams;
    private List<ApiHeader> headers;

    /**
     * NEW — actual Java Type to be resolved by SchemaResolver
     */
    private Type requestBodyType;

    /**
     * NEW — actual Java Type to be resolved by SchemaResolver
     */
    private Type responseBodyType;

    /**
     * NEW — each exception handler return type corresponds to one entry
     */
    private final List<Type> errorBodyTypes = new ArrayList<>();

    private boolean deprecated;

    // ───────────────────────────────
    // Getters
    // ───────────────────────────────

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public List<String> getProduces() {
        return produces;
    }

    public List<ApiPathVariable> getPathVariables() {
        return pathVariables;
    }

    public List<ApiParam> getQueryParams() {
        return queryParams;
    }

    public List<ApiHeader> getHeaders() {
        return headers;
    }

    public Type getRequestBodyType() {
        return requestBodyType;
    }

    public Type getResponseBodyType() {
        return responseBodyType;
    }

    public List<Type> getErrorBodyTypes() {
        return errorBodyTypes;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    // ───────────────────────────────
    // Mutators / Fluent setters
    // ───────────────────────────────

    public ApiEndpoint setName(String name) {
        this.name = name;
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public ApiEndpoint setDescription(String description) {
        this.description = description;
        return this;
    }

    public void secure() {
        this.isSecured = true;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public void setPathVariables(List<ApiPathVariable> pathVariables) {
        this.pathVariables = pathVariables;
    }

    public void setQueryParams(List<ApiParam> queryParams) {
        this.queryParams = queryParams;
    }

    public void setHeaders(List<ApiHeader> headers) {
        this.headers = headers;
    }

    /**
     * NEW — required for correct schema processing
     */
    public void setRequestBodyType(Type type) {
        this.requestBodyType = type;
    }

    /**
     * NEW — required for correct schema processing
     */
    public void setResponseBodyType(Type type) {
        this.responseBodyType = type;
    }

    /**
     * NEW — append exception response schema types
     */
    public void addErrorBodyType(Type type) {
        this.errorBodyTypes.add(type);
    }

    public void deprecate() {
        this.deprecated = true;
    }
}
