package test.retreever.boot;

import dev.retreever.api.RetreeverAuthenticationController;
import dev.retreever.api.RetreeverController;
import dev.retreever.api.RetreeverUIController;
import dev.retreever.api.config.RetreeverSecurityHeadersFilter;
import dev.retreever.auth.RetreeverAuthenticationFilter;
import dev.retreever.boot.RetreeverBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverDisabledIntegrationTest.TestApplication.class,
        properties = "retreever.enabled=false"
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverDisabledIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retreeverBeansAreNotRegisteredWhenDisabled() {
        assertThat(context.getBeansOfType(RetreeverController.class)).isEmpty();
        assertThat(context.getBeansOfType(RetreeverUIController.class)).isEmpty();
        assertThat(context.getBeansOfType(RetreeverAuthenticationController.class)).isEmpty();
        assertThat(context.getBeansOfType(RetreeverSecurityHeadersFilter.class)).isEmpty();
        assertThat(context.getBeansOfType(RetreeverAuthenticationFilter.class)).isEmpty();
        assertThat(context.getBeansOfType(RetreeverBootstrap.class)).isEmpty();
    }

    @Test
    void hostApplicationRoutesStillWorkWhenRetreeverIsDisabled() throws Exception {
        mockMvc.perform(get("/host-health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void retreeverUiAndDataRoutesAreUnavailableWhenDisabled() throws Exception {
        mockMvc.perform(get("/retreever").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Content-Security-Policy"));

        mockMvc.perform(get("/retreever/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/workspace").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/doc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/environment").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void retreeverAuthEndpointsAreUnavailableWhenDisabled() throws Exception {
        mockMvc.perform(post("/retreever/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"admin","password":"secret"}
                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/retreever/refresh"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/retreever/logout"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retreeverAssetsAreUnavailableWhenDisabled() throws Exception {
        mockMvc.perform(get("/retreever/assets/icons/icon192v2.png"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/retreever/images/github.svg"))
                .andExpect(status().isNotFound());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(HostController.class)
    static class TestApplication {
    }

    @RestController
    static class HostController {

        @GetMapping("/host-health")
        String health() {
            return "ok";
        }
    }
}
