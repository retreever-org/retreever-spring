package dev.retreever.api.config;

import dev.retreever.auth.RetreeverAuthSupport;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetreeverResourceConfig implements WebMvcConfigurer {

    private static final String UI_BASE_PATH = RetreeverAuthSupport.RETREEVER_BASE_PATH;
    private static final String UI_RESOURCE_LOCATION = "classpath:/META-INF/retreever-ui/retreever/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler(
                        UI_BASE_PATH + "/favicon.ico",
                        UI_BASE_PATH + "/manifest.json",
                        UI_BASE_PATH + "/sw.js"
                )
                .addResourceLocations(
                        UI_RESOURCE_LOCATION
                )
                .setCacheControl(CacheControl.noCache());

        registry
                .addResourceHandler(UI_BASE_PATH + "/assets/**")
                .addResourceLocations(UI_RESOURCE_LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry
                .addResourceHandler(UI_BASE_PATH + "/images/**")
                .addResourceLocations(UI_RESOURCE_LOCATION + "images/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
