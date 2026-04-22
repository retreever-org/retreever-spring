package dev.retreever.boot;

import dev.retreever.annotation.ApiEndpoint;
import dev.retreever.annotation.ApiError;
import dev.retreever.view.dto.ApiDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RetreeverResponseBodyDocumentationIntegrationTest.TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverResponseBodyDocumentationIntegrationTest {

    @Autowired
    private RetreeverBootstrap bootstrap;

    @Test
    void documentsClassLevelAndMethodLevelResponseBodyControllersAndAdvices() {
        ApiDocument document = bootstrap.getDocument();

        ApiDocument.Endpoint classLevelEndpoint = findEndpoint(document, "/class-response/api").orElse(null);
        ApiDocument.Endpoint methodLevelEndpoint = findEndpoint(document, "/method-response/api").orElse(null);
        ApiDocument.Endpoint inferredErrorEndpoint = findEndpoint(document, "/implicit-error/api").orElse(null);

        assertThat(classLevelEndpoint).isNotNull();
        assertThat(classLevelEndpoint.errors())
                .extracting(ApiDocument.Error::statusCode)
                .containsExactly(HttpStatus.NOT_FOUND.value());

        assertThat(methodLevelEndpoint).isNotNull();
        assertThat(methodLevelEndpoint.produces()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(methodLevelEndpoint.produces()).doesNotContain(MediaType.TEXT_HTML_VALUE);
        assertThat(methodLevelEndpoint.errors())
                .extracting(ApiDocument.Error::statusCode)
                .containsExactly(HttpStatus.BAD_REQUEST.value());

        assertThat(inferredErrorEndpoint).isNotNull();
        assertThat(inferredErrorEndpoint.errors())
                .extracting(ApiDocument.Error::statusCode)
                .containsExactly(HttpStatus.CONFLICT.value());

        assertThat(findEndpoint(document, "/method-response/page")).isEmpty();
    }

    private Optional<ApiDocument.Endpoint> findEndpoint(ApiDocument document, String path) {
        return document.groups().stream()
                .flatMap(group -> group.endpoints().stream())
                .filter(endpoint -> endpoint.path().equals(path))
                .findFirst();
    }

    @SpringBootApplication
    @Import({
            ClassLevelResponseBodyController.class,
            MethodLevelResponseBodyController.class,
            ImplicitExceptionHandlerController.class,
            ClassLevelResponseBodyAdvice.class,
            MethodLevelResponseBodyAdvice.class,
            ImplicitExceptionHandlerAdvice.class
    })
    static class TestApplication {
    }

    @Controller
    @ResponseBody
    @RequestMapping("/class-response")
    static class ClassLevelResponseBodyController {

        @GetMapping("/api")
        @ApiEndpoint(errors = ClassLevelResponseBodyException.class)
        ClassLevelPayload api() {
            return new ClassLevelPayload("class-level");
        }
    }

    @Controller
    @RequestMapping("/method-response")
    static class MethodLevelResponseBodyController {

        @GetMapping("/api")
        @ResponseBody
        @ApiEndpoint(errors = MethodLevelResponseBodyException.class)
        String api() {
            return "method-level";
        }

        @GetMapping("/page")
        String page() {
            return "dashboard";
        }
    }

    @Controller
    @ResponseBody
    @RequestMapping("/implicit-error")
    static class ImplicitExceptionHandlerController {

        @GetMapping("/api")
        @ApiEndpoint(errors = ImplicitException.class)
        String api() {
            return "implicit-error";
        }
    }

    @ControllerAdvice
    @ResponseBody
    static class ClassLevelResponseBodyAdvice {

        @ExceptionHandler(ClassLevelResponseBodyException.class)
        @ApiError(status = HttpStatus.NOT_FOUND, description = "Class-level advice")
        ErrorPayload handle(ClassLevelResponseBodyException ex) {
            return new ErrorPayload(ex.getMessage());
        }
    }

    @ControllerAdvice
    static class MethodLevelResponseBodyAdvice {

        @ExceptionHandler(MethodLevelResponseBodyException.class)
        @ResponseBody
        @ApiError(status = HttpStatus.BAD_REQUEST, description = "Method-level advice")
        ErrorPayload handle(MethodLevelResponseBodyException ex) {
            return new ErrorPayload(ex.getMessage());
        }
    }

    @ControllerAdvice
    @ResponseBody
    static class ImplicitExceptionHandlerAdvice {

        @ExceptionHandler
        @ApiError(status = HttpStatus.CONFLICT, description = "Implicit exception type")
        ErrorPayload handle(ImplicitException ex) {
            return new ErrorPayload(ex.getMessage());
        }
    }

    record ClassLevelPayload(String message) {
    }

    record ErrorPayload(String message) {
    }

    static class ClassLevelResponseBodyException extends RuntimeException {
        ClassLevelResponseBodyException() {
            super("class-level");
        }
    }

    static class MethodLevelResponseBodyException extends RuntimeException {
        MethodLevelResponseBodyException() {
            super("method-level");
        }
    }

    static class ImplicitException extends RuntimeException {
        ImplicitException() {
            super("implicit");
        }
    }
}
