package dev.retreever.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "retreever.env")
@Component
public class TestEnvironmentConfig implements InitializingBean {

    private List<Variable> variables;

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    // ---------------------------- VALIDATION LOGIC ----------------------------

    @Override
    public void afterPropertiesSet() {
        if (variables == null || variables.isEmpty()) return;

        for (Variable var : variables) {
            validateVariable(var);
        }
    }

    private void validateVariable(Variable variable) {
        Source source = variable.getSource();
        if (source == null) {
            throw new IllegalArgumentException(
                    "Environment variable '" + variable.getName() + "' must define a source."
            );
        }

        boolean hasValue = source.getValue() != null;
        boolean hasRequest = source.getRequest() != null;

        // Fail if BOTH value and request are provided
        if (hasValue && hasRequest) {
            throw new IllegalArgumentException(
                    "Environment variable '" + variable.getName() +
                            "' cannot define both 'value' and 'request'. Only one source type is permitted."
            );
        }

        // If request exists, validate response paths
        if (hasRequest) {
            Request req = source.getRequest();
            Response res = req.getResponse();

            if (res == null) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variable.getName() +
                                "' using request must define a response block."
                );
            }

            boolean hasBodyPaths = hasEntries(res.getBodyAttributePaths());
            boolean hasHeaderPaths = hasEntries(res.getHeaderAttributePaths());

            if (hasBodyPaths && hasHeaderPaths) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variable.getName() +
                                "' cannot define both body attribute paths and header attribute paths. Only one response source is permitted."
                );
            }

            if (!hasBodyPaths && !hasHeaderPaths) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variable.getName() +
                                "' must define either a body attribute path or a header attribute path."
                );
            }
        }
    }

    private boolean hasEntries(List<String> values) {
        return values != null && values.stream().anyMatch(StringUtils::hasText);
    }

    // -------------------------------------- DATA MODELS -------------------------------------

    public static class Variable {
        private String name;
        private Source source;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
    }

    public static class Source {
        private String value;
        private Request request;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public Request getRequest() { return request; }
        public void setRequest(Request request) { this.request = request; }
    }

    public static class Request {
        private Set<String> endpoints;
        private String method;
        private Response response;

        public Set<String> getEndpoints() { return endpoints; }
        public void setEndpoints(Set<String> endpoints) { this.endpoints = endpoints; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public Response getResponse() { return response; }
        public void setResponse(Response response) { this.response = response; }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Response {
        private String bodyAttributePath;
        @JsonProperty("body_attribute_paths")
        private List<String> bodyAttributePaths;
        private String headerAttributePath;
        @JsonProperty("header_attribute_paths")
        private List<String> headerAttributePaths;

        @JsonIgnore
        public String getBodyAttributePath() { return bodyAttributePath; }
        public void setBodyAttributePath(String path) { this.bodyAttributePath = path; }

        public List<String> getBodyAttributePaths() { return paths(bodyAttributePath, bodyAttributePaths); }
        public void setBodyAttributePaths(List<String> paths) { this.bodyAttributePaths = paths; }

        @JsonIgnore
        public String getHeaderAttributePath() { return headerAttributePath; }
        public void setHeaderAttributePath(String path) { this.headerAttributePath = path; }

        public List<String> getHeaderAttributePaths() { return paths(headerAttributePath, headerAttributePaths); }
        public void setHeaderAttributePaths(List<String> paths) { this.headerAttributePaths = paths; }

        private List<String> paths(String path, List<String> paths) {
            if (!StringUtils.hasText(path)) {
                return paths;
            }
            if (paths == null || paths.isEmpty()) {
                return List.of(path);
            }
            return java.util.stream.Stream.concat(java.util.stream.Stream.of(path), paths.stream())
                    .filter(StringUtils::hasText)
                    .toList();
        }
    }
}
