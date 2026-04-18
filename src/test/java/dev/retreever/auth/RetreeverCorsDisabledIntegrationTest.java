package dev.retreever.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RetreeverCorsDisabledIntegrationTest.TestApplication.class,
        properties = {
                "retreever.auth.username=admin",
                "retreever.auth.password=secret"
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverCorsDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotEmitCorsHeadersWhenDevCrossOriginSupportIsNotConfigured() throws Exception {
        mockMvc.perform(get("/retreever/doc")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
