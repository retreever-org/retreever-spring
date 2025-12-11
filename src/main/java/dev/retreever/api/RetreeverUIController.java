package dev.retreever.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
