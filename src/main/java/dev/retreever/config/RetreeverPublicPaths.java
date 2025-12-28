package dev.retreever.config;

public record RetreeverPublicPaths(
        String[] paths
) {
    public RetreeverPublicPaths() {
        this(new String[]{
                "/retreever/**",
                "/index.html",
                "/favicon.ico",
                "/manifest.json",
                "/assets/**",
                "/images/**",
                "/ws.js"
        });
    }

    public static String[] get() {
       return new RetreeverPublicPaths().paths;
    }
}
