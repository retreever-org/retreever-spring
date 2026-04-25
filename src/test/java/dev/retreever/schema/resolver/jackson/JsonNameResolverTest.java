package dev.retreever.schema.resolver.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import dev.retreever.schema.model.ObjectSchema;
import dev.retreever.schema.resolver.SchemaResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNameResolverTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void resolvesAllCapsFieldNamesLikeJacksonSerialization() {
        ObjectSchema schema = resolve(AllCapsPayload.class);
        Map<String, Object> serialized = mapper.convertValue(
                new AllCapsPayload("123456", "7890"),
                new TypeReference<>() {
                }
        );

        assertThat(serialized).containsKeys("otp", "pin");
        assertThat(schema.getProperties().keySet())
                .contains("otp", "pin")
                .doesNotContain("OTP", "PIN");
    }

    @Test
    void resolvesExplicitJsonPropertyNamesThroughJackson() {
        ObjectSchema schema = resolve(ExplicitJsonPropertyPayload.class);

        assertThat(schema.getProperties().keySet())
                .contains("otp_code")
                .doesNotContain("OTP");
    }

    @Test
    void resolvesJsonNamingStrategyThroughJackson() {
        ObjectSchema schema = resolve(SnakeCasePayload.class);

        assertThat(schema.getProperties().keySet())
                .contains("user_otp")
                .doesNotContain("userOTP");
    }

    @Test
    void usesConfiguredObjectMapperNamingStrategy() {
        ObjectMapper customMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        JsonNameResolver.configure(customMapper);
        try {
            ObjectSchema schema = resolve(GlobalSnakeCasePayload.class);

            assertThat(schema.getProperties().keySet())
                    .contains("user_otp")
                    .doesNotContain("userOTP");
        } finally {
            JsonNameResolver.configure(new ObjectMapper());
        }
    }

    @Test
    void ignoresPropertiesByResolvedJsonName() {
        ObjectSchema schema = resolve(IgnoredJsonNamePayload.class);

        assertThat(schema.getProperties().keySet())
                .contains("pin")
                .doesNotContain("otp");
    }

    private ObjectSchema resolve(Class<?> type) {
        return (ObjectSchema) SchemaResolver.initResolution(type);
    }

    static class AllCapsPayload {

        private final String OTP;
        private final String PIN;

        AllCapsPayload(String OTP, String PIN) {
            this.OTP = OTP;
            this.PIN = PIN;
        }

        public String getOTP() {
            return OTP;
        }

        public String getPIN() {
            return PIN;
        }
    }

    static class ExplicitJsonPropertyPayload {

        @JsonProperty("otp_code")
        private String OTP;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class SnakeCasePayload {

        private String userOTP;

        public String getUserOTP() {
            return userOTP;
        }
    }

    @JsonIgnoreProperties("otp")
    static class IgnoredJsonNamePayload {

        private String OTP;
        private String PIN;

        public String getOTP() {
            return OTP;
        }

        public String getPIN() {
            return PIN;
        }
    }

    static class GlobalSnakeCasePayload {

        private String userOTP;

        public String getUserOTP() {
            return userOTP;
        }
    }
}
