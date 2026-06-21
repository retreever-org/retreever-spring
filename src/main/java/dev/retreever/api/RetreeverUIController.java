package dev.retreever.api;

import dev.retreever.auth.RetreeverAuthSupport;
import dev.retreever.boot.RetreeverBasePathResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class RetreeverUIController {

    private static final String RETREEVER_INDEX_LOCATION = "META-INF/retreever-ui/retreever/index.html";
    private static final String ROOT_DIV_MARKER = "<div id=\"root\"></div>";
    private static final String CONTEXT_PATH_BOOTSTRAP_TEMPLATE = """
            <script>
              window.__RETREEVER_CONTEXT_PATH__ = "%s";
            </script>
            """;

    private final RetreeverBasePathResolver basePathResolver;

    public RetreeverUIController(RetreeverBasePathResolver basePathResolver) {
        this.basePathResolver = basePathResolver;
    }

    @GetMapping({
            RetreeverAuthSupport.RETREEVER_BASE_PATH,
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/index.html",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/login",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/workspace",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/device-requirements"
    })
    public ResponseEntity<String> appShell(HttpServletRequest request) throws IOException {
        Resource resource = new ClassPathResource(RETREEVER_INDEX_LOCATION);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String uiPath = basePathResolver.resolveRetreeverUiPath(request);
        String contextPath = escapeForJavaScript(basePathResolver.resolveContextPath(request));
        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8)
                .replace(RetreeverAuthSupport.RETREEVER_BASE_PATH, uiPath)
                .replace(ROOT_DIV_MARKER, ROOT_DIV_MARKER + "\n" + CONTEXT_PATH_BOOTSTRAP_TEMPLATE.formatted(contextPath));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("Vary", RetreeverBasePathResolver.FORWARDED_PREFIX_HEADER)
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    private String escapeForJavaScript(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
