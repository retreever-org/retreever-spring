package dev.retreever.boot;

import dev.retreever.auth.RetreeverAuthSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverBasePathIntegrationTest.TestApplication.class,
        properties = {
                "retreever.context-path=/dist-prod",
                "retreever.auth.username=admin",
                "retreever.auth.password=secret"
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverBasePathIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredContextPathRewritesStudioHtmlAndInjectsRuntimeContextPath() throws Exception {
        assumePackagedUiPresent();

        mockMvc.perform(get("/retreever").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dist-prod/retreever/assets/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.__RETREEVER_CONTEXT_PATH__ = \"/dist-prod\"")));

        mockMvc.perform(get("/retreever/assets/" + packagedAssetName()).accept(MediaType.valueOf("application/javascript")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/dist-prod/retreever/doc"))))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("immutable")));
    }

    @Test
    void configuredContextPathIsUsedForAuthCookiePaths() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/retreever/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(cookieHeader(loginResult, RetreeverAuthSupport.ACCESS_TOKEN_COOKIE_NAME))
                .contains("Path=/dist-prod/retreever");
        assertThat(cookieHeader(loginResult, RetreeverAuthSupport.REFRESH_TOKEN_COOKIE_NAME))
                .contains("Path=/dist-prod/retreever");
        assertThat(cookieHeader(loginResult, RetreeverAuthSupport.DEVICE_ID_COOKIE_NAME))
                .contains("Path=/dist-prod/retreever");
    }

    private String packagedAssetName() throws Exception {
        assumePackagedUiPresent();

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/META-INF/retreever-ui/retreever/assets/*.js");

        Assumptions.assumeTrue(resources.length > 0, "No packaged JavaScript asset found");

        String fileName = resources[0].getFilename();
        Assumptions.assumeTrue(fileName != null && !fileName.isBlank(), "Packaged JavaScript asset must have a filename");
        return fileName;
    }

    private void assumePackagedUiPresent() {
        Assumptions.assumeTrue(
                new ClassPathResource("META-INF/retreever-ui/retreever/index.html").exists(),
                "Packaged Retreever UI is not available in this test run"
        );
    }

    private String cookieHeader(MvcResult result, String cookieName) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cookie header for " + cookieName));
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
