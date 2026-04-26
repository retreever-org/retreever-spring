package dev.retreever.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverPublicPathsTest {

    @Test
    void includesChromeDevToolsProbePathToAvoidBasicAuthPopup() {
        assertThat(RetreeverPublicPaths.get())
                .contains("/.well-known/appspecific/com.chrome.devtools.json")
                .doesNotContain("/.well-known/**");
    }
}
