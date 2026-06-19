package dev.retreever.endpoint.resolver;

import dev.retreever.endpoint.model.ApiEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointPathAndMethodResolverTest {

    @Test
    void resolvesPlaceholdersInClassAndMethodMappingPaths() throws Exception {
        ApiEndpoint endpoint = resolve(
                PlaceholderController.class,
                "users",
                value -> value
                        .replace("${api.base}", "/api/v1")
                        .replace("${users.path}", "/users/{user_id}")
        );

        assertThat(endpoint.getPath()).isEqualTo("/api/v1/users/{user_id}");
        assertThat(endpoint.getHttpMethod()).isEqualTo("GET");
    }

    @Test
    void readsPathAliasesWhenValueAliasIsEmpty() throws Exception {
        ApiEndpoint endpoint = resolve(
                PathAliasController.class,
                "reports",
                value -> value
                        .replace("${api.base}", "/api/v1")
                        .replace("${reports.path}", "/reports")
        );

        assertThat(endpoint.getPath()).isEqualTo("/api/v1/reports");
    }

    @Test
    void preservesOriginalPathWhenPlaceholderCannotBeResolved() throws Exception {
        ApiEndpoint endpoint = resolve(
                UnresolvedPlaceholderController.class,
                "users",
                value -> {
                    throw new IllegalArgumentException("Unresolved placeholder");
                }
        );

        assertThat(endpoint.getPath()).isEqualTo("/${missing.base}/users");
    }

    private ApiEndpoint resolve(
            Class<?> controllerClass,
            String methodName,
            StringValueResolver valueResolver) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName);
        ApiEndpoint endpoint = new ApiEndpoint();

        EndpointPathAndMethodResolver.resolve(endpoint, method, valueResolver);

        return endpoint;
    }

    @RequestMapping("${api.base}")
    static class PlaceholderController {

        @GetMapping("${users.path}")
        void users() {
        }
    }

    @RequestMapping(path = "${api.base}")
    static class PathAliasController {

        @GetMapping(path = "${reports.path}")
        void reports() {
        }
    }

    @RequestMapping("${missing.base}")
    static class UnresolvedPlaceholderController {

        @GetMapping("/users")
        void users() {
        }
    }
}
