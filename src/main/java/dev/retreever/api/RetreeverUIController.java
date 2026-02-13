package dev.retreever.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.TimeUnit;

@Controller
public class RetreeverUIController {

    @RequestMapping("/retreever")
    public String entryPoint() {
        return "forward:/index.html";
    }

    @RequestMapping("/retreever/**")
    public String forwardReactRoutes() {
        return "forward:/index.html";
    }

    @GetMapping("/assets/icons/{name}")
    public ResponseEntity<Resource> icon(@PathVariable String name) {
        Resource r = new ClassPathResource("static/assets/icons/" + name);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).immutable())
                .body(r);
    }

}
