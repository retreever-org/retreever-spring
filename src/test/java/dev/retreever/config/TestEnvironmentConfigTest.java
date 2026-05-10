package dev.retreever.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestEnvironmentConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsPluralBodyAttributePaths() {
        TestEnvironmentConfig config = configWith(responseWithBodyPaths("data.access_token", "accessToken"));

        config.afterPropertiesSet();

        assertThat(config.getVariables().get(0).getSource().getRequest().getResponse().getBodyAttributePaths())
                .containsExactly("data.access_token", "accessToken");
    }

    @Test
    void acceptsSingularBodyAttributePathForBackwardCompatibility() {
        TestEnvironmentConfig config = configWith(responseWithBodyPath("data.access_token"));

        config.afterPropertiesSet();

        assertThat(config.getVariables().get(0).getSource().getRequest().getResponse().getBodyAttributePath())
                .isEqualTo("data.access_token");
    }

    @Test
    void acceptsPluralHeaderAttributePaths() {
        TestEnvironmentConfig config = configWith(responseWithHeaderPaths("Authorization", "X-Access-Token"));

        config.afterPropertiesSet();

        assertThat(config.getVariables().get(0).getSource().getRequest().getResponse().getHeaderAttributePaths())
                .containsExactly("Authorization", "X-Access-Token");
    }

    @Test
    void allowsDirectValueWithoutRequestAutomation() {
        TestEnvironmentConfig.Source source = new TestEnvironmentConfig.Source();
        source.setValue("device-web-001");

        TestEnvironmentConfig.Variable variable = new TestEnvironmentConfig.Variable();
        variable.setName("device-id");
        variable.setSource(source);

        TestEnvironmentConfig config = new TestEnvironmentConfig();
        config.setVariables(List.of(variable));

        config.afterPropertiesSet();

        assertThat(config.getVariables().get(0).getSource().getValue()).isEqualTo("device-web-001");
    }

    @Test
    void serializesOnlyPluralResponsePathFields() throws Exception {
        TestEnvironmentConfig config = configWith(responseWithBodyPath("data.access_token"));

        String json = objectMapper.writeValueAsString(config);

        assertThat(json).contains("\"body_attribute_paths\":[\"data.access_token\"]");
        assertThat(json).doesNotContain("body_attribute_path\"");
        assertThat(json).doesNotContain("header_attribute_path\"");
        assertThat(json).doesNotContain("bodyAttributePath");
        assertThat(json).doesNotContain("headerAttributePath");
    }

    @Test
    void rejectsBodyAndHeaderAttributePathsTogether() {
        TestEnvironmentConfig.Response response = responseWithBodyPaths("data.access_token");
        response.setHeaderAttributePaths(List.of("Authorization"));
        TestEnvironmentConfig config = configWith(response);

        assertThatThrownBy(config::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot define both body attribute paths and header attribute paths");
    }

    @Test
    void rejectsRequestSourceWithoutUsableResponsePath() {
        TestEnvironmentConfig.Response response = new TestEnvironmentConfig.Response();
        response.setBodyAttributePaths(List.of(" ", ""));
        TestEnvironmentConfig config = configWith(response);

        assertThatThrownBy(config::afterPropertiesSet)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must define either a body attribute path or a header attribute path");
    }

    private TestEnvironmentConfig configWith(TestEnvironmentConfig.Response response) {
        TestEnvironmentConfig.Request request = new TestEnvironmentConfig.Request();
        request.setEndpoints(Set.of("/api/v1/public/login", "/api/v1/public/login/refresh"));
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

    private TestEnvironmentConfig.Response responseWithBodyPaths(String... paths) {
        TestEnvironmentConfig.Response response = new TestEnvironmentConfig.Response();
        response.setBodyAttributePaths(List.of(paths));
        return response;
    }

    private TestEnvironmentConfig.Response responseWithHeaderPaths(String... paths) {
        TestEnvironmentConfig.Response response = new TestEnvironmentConfig.Response();
        response.setHeaderAttributePaths(List.of(paths));
        return response;
    }
}
