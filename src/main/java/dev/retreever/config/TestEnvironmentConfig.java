package dev.retreever.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "retreever.env")
@Component
public class TestEnvironmentConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(TestEnvironmentConfig.class);
    private static final Pattern PREFIXED_VALUE = Pattern.compile("^\\[([^]]+)]\\s*(.+)$");

    private Variable variable;
    private List<Variable> variables;

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    // ---------------------------- VALIDATION LOGIC ----------------------------

    @Override
    public void afterPropertiesSet() {
        List<Variable> configuredVariables = getConfiguredVariables();
        if (configuredVariables.isEmpty()) return;

        for (Variable var : configuredVariables) {
            try {
                validateVariable(var);
            } catch (IllegalArgumentException ex) {
                log.error("Invalid Retreever environment variable configuration. The invalid variable will be ignored.", ex);
                removeVariable(var);
            }
        }
    }

    private void removeVariable(Variable invalidVariable) {
        if (variable == invalidVariable) {
            variable = null;
        }
        if (variables != null) {
            variables = variables.stream()
                    .filter(candidate -> candidate != invalidVariable)
                    .toList();
        }
    }

    private void validateVariable(Variable variable) {
        if (!StringUtils.hasText(variable.getName())) {
            throw new IllegalArgumentException(
                    "Environment variable must define a name."
            );
        }

        boolean hasValue = StringUtils.hasText(variable.getResolvedValue());
        boolean hasFrom = variable.getResolvedFrom() != null;

        if (hasValue && hasFrom) {
            throw new IllegalArgumentException(
                    "Environment variable '" + variable.getName() +
                            "' cannot define both 'value' and 'from'. Only one source type is permitted."
            );
        }

        if (!hasValue && !hasFrom) {
            throw new IllegalArgumentException(
                    "Environment variable '" + variable.getName() + "' must define either 'value' or 'from'."
            );
        }

        if (hasFrom) {
            From from = variable.getResolvedFrom();

            if (!hasEndpoints(from.getEndpoints())) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variable.getName() +
                                "' using from must define at least one endpoint."
                );
            }

            if (!hasPaths(from.getExtract())) {
                throw new IllegalArgumentException(
                        "Environment variable '" + variable.getName() +
                                "' using from must define at least one extract path."
                );
            }
        }
    }

    @JsonIgnore
    public List<Variable> getConfiguredVariables() {
        List<Variable> configured = new ArrayList<>();
        if (variable != null) {
            configured.add(variable);
        }
        if (variables != null) {
            variables.stream()
                    .filter(v -> v != null)
                    .forEach(configured::add);
        }
        return configured;
    }

    private boolean hasEndpoints(List<Endpoint> values) {
        return values != null && values.stream()
                .anyMatch(endpoint -> endpoint != null &&
                        StringUtils.hasText(endpoint.getMethod()) &&
                        StringUtils.hasText(endpoint.getUri()));
    }

    private boolean hasPaths(List<ResponsePath> values) {
        return values != null && values.stream()
                .anyMatch(path -> path != null &&
                        StringUtils.hasText(path.getSource()) &&
                        StringUtils.hasText(path.getPath()));
    }

    // -------------------------------------- DATA MODELS -------------------------------------

    public static class Variable {
        private String name;
        private String value;
        private From from;
        private Source source;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public From getFrom() { return from; }
        public void setFrom(From from) { this.from = from; }

        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }

        @JsonIgnore
        public String getResolvedValue() {
            if (StringUtils.hasText(value)) {
                return value;
            }
            return source == null ? null : source.getValue();
        }

        @JsonIgnore
        public From getResolvedFrom() {
            if (from != null) {
                return from;
            }
            return source == null ? null : source.toFrom();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class From {
        private List<Endpoint> endpoints;
        private List<ResponsePath> extract;

        public List<Endpoint> getEndpoints() { return endpoints; }
        public void setEndpoints(List<Endpoint> endpoints) { this.endpoints = endpoints; }

        public List<ResponsePath> getExtract() { return extract; }
        public void setExtract(List<ResponsePath> extract) { this.extract = extract; }
    }

    public static class Source {
        private String value;
        private Api api;
        private Request request;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public Api getApi() {
            if (api != null) {
                return api;
            }
            return request == null ? null : request.toApi();
        }

        public void setApi(Api api) { this.api = api; }

        @JsonIgnore
        public Request getRequest() { return request; }
        public void setRequest(Request request) { this.request = request; }

        From toFrom() {
            Api resolvedApi = getApi();
            if (resolvedApi == null) {
                return null;
            }

            From from = new From();
            from.setEndpoints(resolvedApi.getEndpoints());

            Response response = resolvedApi.getResponse();
            if (response != null) {
                from.setExtract(response.getPaths());
            }

            return from;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Api {
        private List<Endpoint> endpoints;
        private Response response;

        public List<Endpoint> getEndpoints() { return endpoints; }
        public void setEndpoints(List<Endpoint> endpoints) { this.endpoints = endpoints; }

        public Response getResponse() { return response; }
        public void setResponse(Response response) { this.response = response; }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Endpoint {
        private String method;
        private String uri;

        public Endpoint() {
        }

        public Endpoint(String value) {
            Endpoint parsed = parseEndpoint(value);
            this.method = parsed.method;
            this.uri = parsed.uri;
        }

        public Endpoint(String method, String uri) {
            this.method = normalizeMethod(method);
            this.uri = normalizeUri(uri);
        }

        public static Endpoint valueOf(String value) {
            return parseEndpoint(value);
        }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = normalizeMethod(method); }

        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = normalizeUri(uri); }
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

        Api toApi() {
            Api api = new Api();
            api.setResponse(response);

            if (endpoints != null) {
                api.setEndpoints(endpoints.stream()
                        .filter(StringUtils::hasText)
                        .map(endpoint -> new Endpoint(method, endpoint))
                        .toList());
            }

            return api;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Response {
        private List<ResponsePath> paths;
        private String bodyAttributePath;
        private List<String> bodyAttributePaths;
        private String headerAttributePath;
        private List<String> headerAttributePaths;

        public List<ResponsePath> getPaths() {
            if (paths != null && !paths.isEmpty()) {
                return paths;
            }

            List<ResponsePath> resolved = new ArrayList<>();
            paths(bodyAttributePath, bodyAttributePaths).stream()
                    .map(path -> new ResponsePath("BODY", path))
                    .forEach(resolved::add);
            paths(headerAttributePath, headerAttributePaths).stream()
                    .map(path -> new ResponsePath("HEADER", path))
                    .forEach(resolved::add);
            return resolved;
        }

        public void setPaths(List<ResponsePath> paths) { this.paths = paths; }

        @JsonIgnore
        public String getBodyAttributePath() { return bodyAttributePath; }
        public void setBodyAttributePath(String path) { this.bodyAttributePath = path; }

        @JsonIgnore
        public List<String> getBodyAttributePaths() { return paths(bodyAttributePath, bodyAttributePaths); }
        public void setBodyAttributePaths(List<String> paths) { this.bodyAttributePaths = paths; }

        @JsonIgnore
        public String getHeaderAttributePath() { return headerAttributePath; }
        public void setHeaderAttributePath(String path) { this.headerAttributePath = path; }

        @JsonIgnore
        public List<String> getHeaderAttributePaths() { return paths(headerAttributePath, headerAttributePaths); }
        public void setHeaderAttributePaths(List<String> paths) { this.headerAttributePaths = paths; }

        private List<String> paths(String path, List<String> paths) {
            if (!StringUtils.hasText(path)) {
                return paths == null ? List.of() : paths.stream()
                        .filter(StringUtils::hasText)
                        .toList();
            }
            if (paths == null || paths.isEmpty()) {
                return List.of(path);
            }
            return java.util.stream.Stream.concat(java.util.stream.Stream.of(path), paths.stream())
                    .filter(StringUtils::hasText)
                    .toList();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ResponsePath {
        private String source;
        private String path;

        public ResponsePath() {
        }

        public ResponsePath(String value) {
            ResponsePath parsed = parsePath(value);
            this.source = parsed.source;
            this.path = parsed.path;
        }

        public ResponsePath(String source, String path) {
            this.source = normalizeSource(source);
            this.path = path;
        }

        public static ResponsePath valueOf(String value) {
            return parsePath(value);
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = normalizeSource(source); }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    private static Endpoint parseEndpoint(String value) {
        Matcher matcher = prefixedValue(value, "endpoint");
        return new Endpoint(normalizeMethod(matcher.group(1)), normalizeUri(matcher.group(2)));
    }

    private static ResponsePath parsePath(String value) {
        Matcher matcher = prefixedValue(value, "response path");
        return new ResponsePath(normalizeSource(matcher.group(1)), matcher.group(2).trim());
    }

    private static Matcher prefixedValue(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Environment " + label + " must not be blank.");
        }

        Matcher matcher = PREFIXED_VALUE.matcher(value.trim());
        if (!matcher.matches() || !StringUtils.hasText(matcher.group(1)) || !StringUtils.hasText(matcher.group(2))) {
            throw new IllegalArgumentException(
                    "Environment " + label + " must use '[TYPE]value' format."
            );
        }
        return matcher;
    }

    private static String normalizeMethod(String method) {
        return StringUtils.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : method;
    }

    private static String normalizeUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return uri;
        }
        String value = uri.trim();
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if ("HEADERS".equals(normalized) || "HEADER/HEADERS".equals(normalized)) {
            return "HEADER";
        }
        if (!"BODY".equals(normalized) && !"HEADER".equals(normalized)) {
            throw new IllegalArgumentException("Environment response path source must be BODY or HEADER.");
        }
        return normalized;
    }
}
