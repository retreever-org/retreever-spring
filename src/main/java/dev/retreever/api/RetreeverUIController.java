package dev.retreever.api;

import dev.retreever.auth.RetreeverAuthSupport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RetreeverUIController {

    private static final String RETREEVER_INDEX_LOCATION = "META-INF/resources/retreever/index.html";

    @GetMapping({
            RetreeverAuthSupport.RETREEVER_BASE_PATH,
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/index.html",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/login",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/workspace",
            RetreeverAuthSupport.RETREEVER_BASE_PATH + "/device-requirements"
    })
    public ResponseEntity<Resource> appShell() {
        Resource indexHtml = new ClassPathResource(RETREEVER_INDEX_LOCATION);

        if (!indexHtml.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
