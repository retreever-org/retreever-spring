package dev.retreever.config;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Distinguishes local exploded-classpath development from packaged dependency usage.
 */
final class RetreeverRuntimeMode {

    boolean isLocalDevelopmentRuntime() {
        URL location = resolveCodeSourceLocation();
        if (location == null) {
            return false;
        }

        try {
            return Files.isDirectory(Path.of(location.toURI()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private URL resolveCodeSourceLocation() {
        if (RetreeverRuntimeMode.class.getProtectionDomain() == null
                || RetreeverRuntimeMode.class.getProtectionDomain().getCodeSource() == null) {
            return null;
        }
        return RetreeverRuntimeMode.class.getProtectionDomain().getCodeSource().getLocation();
    }
}
