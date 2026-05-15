package test.retreever.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = RetreeverSecurityHintLoggingIntegrationTest.TestApplication.class,
        properties = {
                "retreever.auth.username=admin",
                "retreever.auth.password=secret"
        }
)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverSecurityHintLoggingIntegrationTest {

    @Test
    void logsSecurityHintWhenSpringSecurityFilterChainIsPresent(CapturedOutput output) {
        assertThat(output)
                .contains("Retreever detected that Spring Security is enabled in this application.")
                .contains("RetreeverPublicPaths.get()")
                .contains("retreever.security.hint-log=false");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean(name = "springSecurityFilterChain")
        Object springSecurityFilterChain() {
            return new Object();
        }
    }
}
