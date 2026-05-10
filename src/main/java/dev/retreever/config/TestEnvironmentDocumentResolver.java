package dev.retreever.config;

import dev.retreever.view.dto.TestEnvironmentDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class TestEnvironmentDocumentResolver {

    private final TestEnvironmentConfig environmentConfig;

    public TestEnvironmentDocumentResolver(TestEnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    public TestEnvironmentDocument resolve() {
        List<TestEnvironmentDocument.Variable> variables = environmentConfig.getConfiguredVariables().stream()
                .filter(variable -> variable != null && StringUtils.hasText(variable.getName()))
                .map(this::resolveVariable)
                .toList();

        return new TestEnvironmentDocument(variables);
    }

    private TestEnvironmentDocument.Variable resolveVariable(TestEnvironmentConfig.Variable variable) {
        String value = variable.getResolvedValue();
        TestEnvironmentConfig.From from = variable.getResolvedFrom();

        return new TestEnvironmentDocument.Variable(
                variable.getName(),
                StringUtils.hasText(value) ? value : null,
                from == null ? null : resolveFrom(from)
        );
    }

    private TestEnvironmentDocument.From resolveFrom(TestEnvironmentConfig.From from) {
        List<TestEnvironmentDocument.Endpoint> endpoints = from.getEndpoints() == null ? List.of() :
                from.getEndpoints().stream()
                        .filter(endpoint -> endpoint != null &&
                                StringUtils.hasText(endpoint.getMethod()) &&
                                StringUtils.hasText(endpoint.getUri()))
                        .map(endpoint -> new TestEnvironmentDocument.Endpoint(
                                endpoint.getMethod(),
                                endpoint.getUri()
                        ))
                        .toList();

        List<TestEnvironmentDocument.Extraction> extract = from.getExtract() == null ? List.of() :
                from.getExtract().stream()
                        .filter(path -> path != null &&
                                StringUtils.hasText(path.getSource()) &&
                                StringUtils.hasText(path.getPath()))
                        .map(path -> new TestEnvironmentDocument.Extraction(
                                path.getSource(),
                                path.getPath()
                        ))
                        .toList();

        return new TestEnvironmentDocument.From(endpoints, extract);
    }
}
