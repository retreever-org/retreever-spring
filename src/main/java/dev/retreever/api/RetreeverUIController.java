package dev.retreever.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RetreeverUIController {

    @GetMapping("/retreever")
    public String entryPoint() {
        return "forward:/index.html";
    }

    @GetMapping("/retreever/**")
    public String forwardReactRoutes() {
        return "forward:/index.html";
    }
}
