package dev.retreever.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RetreeverResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler(
                        "/index.html",
                        "/manifest.json",
                        "/favicon.ico",
                        "/ws.js",
                        "/assets/**",
                        "/images/**"
                )
                .addResourceLocations(
                        "classpath:/META-INF/resources/",
                        "classpath:/META-INF/resources/assets/",
                        "classpath:/META-INF/resources/images/"
                );
    }
}
