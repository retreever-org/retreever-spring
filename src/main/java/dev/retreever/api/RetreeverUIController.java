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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class RetreeverUIController {

    private static final String RETREEVER_INDEX_LOCATION = "META-INF/retreever-ui/retreever/index.html";
    private static final String RETREEVER_ASSET_LOCATION = "META-INF/retreever-ui/retreever/assets/";

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
        return transformedTextResource(
                new ClassPathResource(RETREEVER_INDEX_LOCATION),
                request,
                MediaType.TEXT_HTML
        );
    }

    @GetMapping(
            path = RetreeverAuthSupport.RETREEVER_BASE_PATH + "/assets/{fileName:.+\\.js}",
            produces = "application/javascript"
    )
    public ResponseEntity<String> javascriptAsset(
            @PathVariable("fileName") String fileName,
            HttpServletRequest request) throws IOException {
        return transformedTextResource(
                new ClassPathResource(RETREEVER_ASSET_LOCATION + fileName),
                request,
                MediaType.valueOf("application/javascript")
        );
    }

    private ResponseEntity<String> transformedTextResource(
            Resource resource,
            HttpServletRequest request,
            MediaType mediaType) throws IOException {
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8)
                .replace(RetreeverAuthSupport.RETREEVER_BASE_PATH, basePathResolver.resolve(request));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(mediaType)
                .body(content);
    }
}
