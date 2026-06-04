package dev.retreever.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverPublicPathsTest {

    @Test
    void onlyIncludesRetreeverOwnedPaths() {
        assertThat(RetreeverPublicPaths.get())
                .containsExactly("/retreever", "/retreever/**")
                .doesNotContain("/.well-known/appspecific/com.chrome.devtools.json")
                .doesNotContain("/.well-known/**");
    }

    @Test
    void doesNotExposeRootUiOrRootAssetPaths() {
        assertThat(RetreeverPublicPaths.get())
                .doesNotContain(
                        "/index.html",
                        "/favicon.ico",
                        "/assets/**",
                        "/images/**"
                );
    }
}
