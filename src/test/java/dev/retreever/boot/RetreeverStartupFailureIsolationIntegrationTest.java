package test.retreever.boot;

import dev.retreever.boot.RetreeverBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverStartupFailureIsolationIntegrationTest.TestApplication.class,
        properties = "retreever.docs.skip[0]=regex:["
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverStartupFailureIsolationIntegrationTest {

    @Autowired
    private RetreeverBootstrap bootstrap;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hostApplicationKeepsRunningWhenRetreeverStartupFails() throws Exception {
        assertThat(bootstrap.isAvailable()).isFalse();
        assertThat(bootstrap.getStartupFailure()).isNotNull();

        mockMvc.perform(get("/host-health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retreever/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SampleController.class)
    static class TestApplication {
    }

    @RestController
    static class SampleController {

        @GetMapping("/host-health")
        String health() {
            return "ok";
        }
    }
}
