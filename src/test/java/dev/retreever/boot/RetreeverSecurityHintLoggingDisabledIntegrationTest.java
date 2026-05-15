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
        classes = RetreeverSecurityHintLoggingDisabledIntegrationTest.TestApplication.class,
        properties = {
                "retreever.auth.username=admin",
                "retreever.auth.password=secret",
                "retreever.security.hint-log=false"
        }
)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetreeverSecurityHintLoggingDisabledIntegrationTest {

    @Test
    void doesNotLogSecurityHintWhenHintLogIsDisabled(CapturedOutput output) {
        assertThat(output)
                .doesNotContain("Retreever detected that Spring Security is enabled in this application.");
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
