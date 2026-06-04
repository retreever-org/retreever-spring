package dev.retreever.config;

public record RetreeverPublicPaths(
        String[] paths
) {
    public RetreeverPublicPaths() {
        this(new String[]{
                "/retreever",
                "/retreever/**"
        });
    }

    public static String[] get() {
       return new RetreeverPublicPaths().paths;
    }
}
