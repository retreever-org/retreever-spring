package dev.retreever.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.retreever.view.dto.TestEnvironmentDocument;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class TestEnvironmentConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsPreferredFromEndpointsAndExtractPaths() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("retreever.env.variables[0].name", "access-token")
                .withProperty("retreever.env.variables[0].from.endpoints[0]", "[Post] /api/v1/public/login")
                .withProperty("retreever.env.variables[0].from.endpoints[1]", "[get]/api/v1/public/login/refresh")
                .withProperty("retreever.env.variables[0].from.extract[0]", "[body] data.access_token")
                .withProperty("retreever.env.variables[0].from.extract[1]", "[Headers] X-Token");

        TestEnvironmentConfig config = Binder.get(environment)
                .bind("retreever.env", Bindable.of(TestEnvironmentConfig.class))
                .get();

        config.afterPropertiesSet();

        TestEnvironmentConfig.From from = config.getVariables().get(0).getFrom();
        assertThat(from.getEndpoints())
                .extracting(TestEnvironmentConfig.Endpoint::getMethod, TestEnvironmentConfig.Endpoint::getUri)
                .containsExactly(
                        tuple("POST", "/api/v1/public/login"),
                        tuple("GET", "/api/v1/public/login/refresh")
                );
        assertThat(from.getExtract())
                .extracting(TestEnvironmentConfig.ResponsePath::getSource, TestEnvironmentConfig.ResponsePath::getPath)
                .containsExactly(
                        tuple("BODY", "data.access_token"),
                        tuple("HEADER", "X-Token")
                );
    }

    @Test
    void bindsSingleStaticVariableShortcut() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("retreever.env.variable.name", "device-id")
                .withProperty("retreever.env.variable.value", "device-web-001");

        TestEnvironmentConfig config = Binder.get(environment)
                .bind("retreever.env", Bindable.of(TestEnvironmentConfig.class))
                .get();

        config.afterPropertiesSet();

        assertThat(config.getConfiguredVariables())
                .extracting(TestEnvironmentConfig.Variable::getName, TestEnvironmentConfig.Variable::getResolvedValue)
                .containsExactly(tuple("device-id", "device-web-001"));
    }

    @Test
    void serializesEnvironmentDocumentWithPreferredFromStructure() throws Exception {
        TestEnvironmentConfig config = configWithPreferredFrom();

        String json = objectMapper.writeValueAsString(resolve(config));

        assertThat(json).contains("\"from\":{\"endpoints\":[{\"method\":\"POST\",\"uri\":\"/api/v1/public/login\"}");
        assertThat(json).contains("\"extract\":[{\"source\":\"BODY\",\"path\":\"data.access_token\"}");
        assertThat(json).doesNotContain("\"source\":{");
        assertThat(json).doesNotContain("\"api\"");
        assertThat(json).doesNotContain("\"request\"");
        assertThat(json).doesNotContain("\"response\"");
        assertThat(json).doesNotContain("body_attribute_paths");
    }

    @Test
    void serializesStaticVariableDocumentWithoutFromBlock() throws Exception {
        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("device-id");
        variable.setValue("device-web-001");

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariable(variable);

        String json = objectMapper.writeValueAsString(resolve(config));

        assertThat(json).contains("\"name\":\"device-id\"");
        assertThat(json).contains("\"value\":\"device-web-001\"");
        assertThat(json).doesNotContain("\"from\"");
    }

    @Test
    void convertsLegacySourceApiConfigurationToFromDocument() throws Exception {
        TestEnvironmentConfig.Api api = new TestEnvironmentConfig.Api();
        api.setEndpoints(List.of(new TestEnvironmentConfig.Endpoint("POST", "/api/v1/public/login")));

        TestEnvironmentConfig.Response response = new TestEnvironmentConfig.Response();
        response.setPaths(List.of(new TestEnvironmentConfig.ResponsePath("BODY", "data.access_token")));
        api.setResponse(response);

        TestEnvironmentConfig.Source source = new TestEnvironmentConfig.Source();
        source.setApi(api);

        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("access-token");
        variable.setSource(source);

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariables(List.of(variable));

        String json = objectMapper.writeValueAsString(resolve(config));

        assertThat(json).contains("\"from\":{\"endpoints\":[{\"method\":\"POST\",\"uri\":\"/api/v1/public/login\"}");
        assertThat(json).contains("\"extract\":[{\"source\":\"BODY\",\"path\":\"data.access_token\"}");
        assertThat(json).doesNotContain("\"api\"");
        assertThat(json).doesNotContain("\"source\":{");
    }

    @Test
    void convertsLegacyRequestConfigurationToFromDocument() throws Exception {
        TestEnvironmentConfig config = configWithLegacyRequest(responseWithBodyPath("data.access_token"));

        config.afterPropertiesSet();

        String json = objectMapper.writeValueAsString(resolve(config));

        assertThat(json).contains("\"from\":{\"endpoints\":[{\"method\":\"POST\",\"uri\":\"/api/v1/public/login\"}");
        assertThat(json).contains("\"extract\":[{\"source\":\"BODY\",\"path\":\"data.access_token\"}");
        assertThat(json).doesNotContain("\"request\"");
        assertThat(json).doesNotContain("body_attribute_path\"");
    }

    @Test
    void ignoresFromSourceWithoutUsableExtractPath() {
        TestEnvironmentConfig.From from = new TestEnvironmentConfig.From();
        from.setEndpoints(List.of(new TestEnvironmentConfig.Endpoint("POST", "/api/v1/public/login")));
        from.setExtract(List.of(new TestEnvironmentConfig.ResponsePath("BODY", " ")));

        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("access-token");
        variable.setFrom(from);

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariables(List.of(variable));

        config.afterPropertiesSet();

        assertThat(config.getConfiguredVariables()).isEmpty();
    }

    private TestEnvironmentDocument resolve(TestEnvironmentConfig config) {
        return new TestEnvironmentDocumentResolver(config).resolve();
    }

    private TestEnvironmentConfig configWithPreferredFrom() {
        TestEnvironmentConfig.From from = new TestEnvironmentConfig.From();
        from.setEndpoints(List.of(
                new TestEnvironmentConfig.Endpoint("POST", "/api/v1/public/login"),
                new TestEnvironmentConfig.Endpoint("GET", "/api/v1/public/login/refresh")
        ));
        from.setExtract(List.of(
                new TestEnvironmentConfig.ResponsePath("BODY", "data.access_token"),
                new TestEnvironmentConfig.ResponsePath("BODY", "accessToken")
        ));

        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("access-token");
        variable.setFrom(from);

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariables(List.of(variable));
        return config;
    }

    private TestEnvironmentConfig configWithLegacyRequest(TestEnvironmentConfig.Response response) {
        TestEnvironmentConfig.Request request = new TestEnvironmentConfig.Request();
        request.setEndpoints(new LinkedHashSet<>(List.of("/api/v1/public/login", "/api/v1/public/login/refresh")));
        request.setMethod("post");
        request.setResponse(response);

        TestEnvironmentConfig.Source source = new TestEnvironmentConfig.Source();
        source.setRequest(request);

        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("access-token");
        variable.setSource(source);

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariables(List.of(variable));
        return config;
    }

    private TestEnvironmentConfig.Response responseWithBodyPath(String path) {
        TestEnvironmentConfig.Response response = new TestEnvironmentConfig.Response();
        response.setBodyAttributePath(path);
        return response;
    }
}
