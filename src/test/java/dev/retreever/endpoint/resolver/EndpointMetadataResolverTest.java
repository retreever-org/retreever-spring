package dev.retreever.endpoint.resolver;

import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointMetadataResolverTest {

    @Test
    void usesApiEndpointSecuredFlagBeforeSecurityAnnotationInference() throws NoSuchMethodException {
        assertThat(resolve(MethodSecurityController.class, "documentedSecuredButPermitAll").isSecured())
                .isTrue();
    }

    @Test
    void resolvesPublicPreAuthorizeExpressions() throws NoSuchMethodException {
        assertThat(resolve(MethodSecurityController.class, "preAuthorizePermitAll").isSecured())
                .isFalse();
        assertThat(resolve(MethodSecurityController.class, "preAuthorizeAnonymous").isSecured())
                .isFalse();
        assertThat(resolve(MethodSecurityController.class, "preAuthorizePublicAlternative").isSecured())
                .isFalse();
    }

    @Test
    void resolvesSecuredPreAuthorizeAndPostAuthorizeExpressions() throws NoSuchMethodException {
        assertThat(resolve(MethodSecurityController.class, "preAuthorizeRole").isSecured())
                .isTrue();
        assertThat(resolve(MethodSecurityController.class, "preAuthorizePublicAndRole").isSecured())
                .isTrue();
        assertThat(resolve(MethodSecurityController.class, "postAuthorizeProtected").isSecured())
                .isTrue();
    }

    @Test
    void resolvesSecuredAndRolesAllowedAnnotations() throws NoSuchMethodException {
        assertThat(resolve(MethodSecurityController.class, "securedRole").isSecured())
                .isTrue();
        assertThat(resolve(MethodSecurityController.class, "securedAnonymous").isSecured())
                .isFalse();
        assertThat(resolve(MethodSecurityController.class, "rolesAllowed").isSecured())
                .isTrue();
    }

    @Test
    void usesClassLevelSecurityWhenMethodHasNoSecurityAnnotation() throws NoSuchMethodException {
        assertThat(resolve(ClassLevelSecurityController.class, "inheritedSecured").isSecured())
                .isTrue();
        assertThat(resolve(ClassLevelSecurityController.class, "methodPublicOverride").isSecured())
                .isFalse();
    }

    private dev.retreever.endpoint.model.ApiEndpoint resolve(
            Class<?> controllerType,
            String methodName
    ) throws NoSuchMethodException {
        Method method = controllerType.getDeclaredMethod(methodName);
        dev.retreever.endpoint.model.ApiEndpoint endpoint = new dev.retreever.endpoint.model.ApiEndpoint();

        EndpointMetadataResolver.resolve(endpoint, method);

        return endpoint;
    }

    static class MethodSecurityController {

        @dev.retreever.annotation.ApiEndpoint(secured = true)
        @PreAuthorize("permitAll()")
        void documentedSecuredButPermitAll() {
        }

        @PreAuthorize("permitAll()")
        void preAuthorizePermitAll() {
        }

        @PreAuthorize("isAnonymous()")
        void preAuthorizeAnonymous() {
        }

        @PreAuthorize("hasRole('ADMIN')")
        void preAuthorizeRole() {
        }

        @PreAuthorize("permitAll() or hasRole('ADMIN')")
        void preAuthorizePublicAlternative() {
        }

        @PreAuthorize("permitAll() and hasRole('ADMIN')")
        void preAuthorizePublicAndRole() {
        }

        @PostAuthorize("returnObject.owner == authentication.name")
        void postAuthorizeProtected() {
        }

        @Secured("ROLE_ADMIN")
        void securedRole() {
        }

        @Secured("IS_AUTHENTICATED_ANONYMOUSLY")
        void securedAnonymous() {
        }

        @RolesAllowed("ADMIN")
        void rolesAllowed() {
        }
    }

    @PreAuthorize("isAuthenticated()")
    static class ClassLevelSecurityController {

        void inheritedSecured() {
        }

        @PreAuthorize("permitAll()")
        void methodPublicOverride() {
        }
    }
}
