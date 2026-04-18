package dev.retreever.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RetreeverAuthDisabledIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverAuthDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedRetreeverApisRemainAccessibleWhenAuthIsNotConfigured() throws Exception {
        mockMvc.perform(get("/retreever/doc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retreever/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retreever/environment").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void authEndpointsReturnNotFoundWhenAuthIsNotConfigured() throws Exception {
        mockMvc.perform(post("/retreever/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"admin","password":"secret"}
                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/retreever/refresh"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/retreever/logout"))
                .andExpect(status().isNotFound());
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
