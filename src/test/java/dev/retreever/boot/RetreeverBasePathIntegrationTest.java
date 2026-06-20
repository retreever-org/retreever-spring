package dev.retreever.boot;

import dev.retreever.auth.RetreeverAuthSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    void configuredContextPathRewritesStudioHtmlAndJavascript() throws Exception {
        mockMvc.perform(get("/retreever").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dist-prod/retreever/assets/")));

        mockMvc.perform(get("/retreever/assets/" + packagedAssetName()).accept(MediaType.valueOf("application/javascript")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dist-prod/retreever/doc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dist-prod/retreever/login")));
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
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/META-INF/retreever-ui/retreever/assets/*.js");

        Assumptions.assumeTrue(resources.length > 0, "No packaged JavaScript asset found");

        String fileName = resources[0].getFilename();
        Assumptions.assumeTrue(fileName != null && !fileName.isBlank(), "Packaged JavaScript asset must have a filename");
        return fileName;
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
