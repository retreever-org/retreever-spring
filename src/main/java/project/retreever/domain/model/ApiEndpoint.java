/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.domain.model;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Represents a single REST endpoint in the Retreever documentation model.
 */
public class ApiEndpoint {
    private String name;                    // typically method name
    private String path;                    // /users/{id}
    private String httpMethod;              // GET, POST, etc.
    private HttpStatus status;              // default response status
    private String description;             // from @RetreeverApi or Javadoc
    private boolean isSecured = false;      // default false unless specified

    private List<String> consumes;
    private List<String> produces;

    private List<ApiPathVariable> pathVariables;   // @PathVariable
    private List<ApiParam> queryParams;     // @RequestParam
    private List<ApiHeader> headers;        // @RequestHeader

    private String requestSchemaRef; // request schema
    private String responseSchemaRef;// success response schema

    private List<String> errorRefs;          // error responses

    private boolean deprecated;             // marking deprecated endpoint

    // ─────────── Getters ───────────

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

    public String getRequestSchemaRef() {
        return requestSchemaRef;
    }

    public String getResponseSchemaRef() {
        return responseSchemaRef;
    }

    public List<String> getErrorRefs() {
        return errorRefs;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    // ─────────── Fluent Setters ───────────

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

    public void setRequestBody(String requestSchemaRef) {
        this.requestSchemaRef = requestSchemaRef;
    }

    public void setResponseBody(String responseSchemaRef) {
        this.responseSchemaRef = responseSchemaRef;
    }

    public void setErrorRefs(List<String> errorRefs) {
        this.errorRefs = errorRefs;
    }

    public void deprecate() {
        this.deprecated = true;
    }
}
