package dev.retreever.boot;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverPackagedUiHtmlTest {

    private static final String PACKAGED_UI_INDEX = "META-INF/retreever-ui/retreever/index.html";

    @Test
    void packagedUiIndexUsesSameOriginAssetTagsWithoutCrossorigin() throws IOException {
        ClassPathResource indexHtml = new ClassPathResource(PACKAGED_UI_INDEX);

        Assumptions.assumeTrue(indexHtml.exists(), "Packaged Retreever UI is not available in this test run");

        String html = StreamUtils.copyToString(indexHtml.getInputStream(), StandardCharsets.UTF_8);

        assertThat(html).contains("defer src=\"/retreever/assets/");
        assertThat(html).doesNotContain("type=\"module\"");
        assertThat(html).doesNotContain("modulepreload");
        assertThat(html).contains("rel=\"stylesheet\"");
        assertThat(html).contains("href=\"/retreever/assets/");
        assertThat(html).doesNotContain("crossorigin");
    }
}
