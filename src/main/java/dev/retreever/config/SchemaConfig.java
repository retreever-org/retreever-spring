package dev.retreever.config;

import java.util.List;

public final class SchemaConfig {

    private static List<String> basePackages;

    public static void init(List<String> packages) {
        basePackages = packages;
    }

    public static List<String> getBasePackages() {
        return basePackages;
    }
}
