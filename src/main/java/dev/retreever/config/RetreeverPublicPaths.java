package dev.retreever.config;

public record RetreeverPublicPaths(
        String[] paths
) {
    public RetreeverPublicPaths() {
        this(new String[]{
                "/retreever",
                "/retreever/**",
                "/index.html",
                "/favicon.ico",
                "/manifest.json",
                "/assets/**",
                "/images/**",
                "/sw.js"
        });
    }

    public static String[] get() {
       return new RetreeverPublicPaths().paths;
    }
}
