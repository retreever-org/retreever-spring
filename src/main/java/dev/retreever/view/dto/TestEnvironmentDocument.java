package dev.retreever.view.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TestEnvironmentDocument(List<Variable> variables) {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Variable(String name, String value, From from) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record From(List<Endpoint> endpoints, List<Extraction> extract) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Endpoint(String method, String uri) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Extraction(String source, String path) {
    }
}
