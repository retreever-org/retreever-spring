package dev.retreever.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetreeverResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler(
                        "/index.html",
                        "/manifest.json",
                        "/favicon.ico",
                        "/sw.js"
                )
                .addResourceLocations(
                        "classpath:/META-INF/resources/"
                )
                .setCacheControl(CacheControl.noCache());

        registry
                .addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/META-INF/resources/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry
                .addResourceHandler("/images/**")
                .addResourceLocations("classpath:/META-INF/resources/images/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
