package dev.retreever.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RetreeverLoginGuardServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-05-17T10:00:00Z"));
    private final RetreeverLoginGuardService loginGuardService =
            new RetreeverLoginGuardService(authProperties(), objectMapper, clock);

    @Test
    void locksEveryFifthFailureWithProgressiveDurations() {
        RetreeverLoginGuardService.LoginGuardState state = loginGuardService.status(new MockHttpServletRequest()).state();

        RetreeverLoginGuardService.FailedLoginResult fifthFailure = failTimes(state, 5);
        assertThat(fifthFailure.locked()).isTrue();
        assertThat(fifthFailure.attemptsLeft()).isZero();
        assertThat(Duration.between(clock.instant(), fifthFailure.lockedUntil())).isEqualTo(Duration.ofSeconds(30));

        clock.advance(Duration.ofSeconds(31));

        RetreeverLoginGuardService.FailedLoginResult tenthFailure = failTimes(fifthFailure.state(), 5);
        assertThat(tenthFailure.locked()).isTrue();
        assertThat(Duration.between(clock.instant(), tenthFailure.lockedUntil())).isEqualTo(Duration.ofMinutes(5));

        clock.advance(Duration.ofMinutes(6));

        RetreeverLoginGuardService.FailedLoginResult fifteenthFailure = failTimes(tenthFailure.state(), 5);
        assertThat(fifteenthFailure.locked()).isTrue();
        assertThat(Duration.between(clock.instant(), fifteenthFailure.lockedUntil())).isEqualTo(Duration.ofMinutes(30));

        clock.advance(Duration.ofMinutes(31));

        RetreeverLoginGuardService.FailedLoginResult twentiethFailure = failTimes(fifteenthFailure.state(), 5);
        assertThat(twentiethFailure.locked()).isTrue();
        assertThat(Duration.between(clock.instant(), twentiethFailure.lockedUntil())).isEqualTo(Duration.ofHours(1));

        clock.advance(Duration.ofMinutes(61));

        RetreeverLoginGuardService.FailedLoginResult twentyFifthFailure = failTimes(twentiethFailure.state(), 5);
        assertThat(twentyFifthFailure.locked()).isTrue();
        assertThat(Duration.between(clock.instant(), twentyFifthFailure.lockedUntil())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void encryptedGuardCookieCanBeReadBackByTheService() {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RetreeverLoginGuardService.FailedLoginResult failedLogin =
                loginGuardService.recordFailedAttempt(loginGuardService.status(firstRequest).state());

        loginGuardService.writeGuardCookie(firstRequest, response, failedLogin.state(), false);

        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setCookies(response.getCookie(RetreeverLoginGuardService.LOGIN_GUARD_COOKIE_NAME));

        RetreeverLoginGuardService.GuardStatus status = loginGuardService.status(secondRequest);

        assertThat(status.locked()).isFalse();
        assertThat(status.state().failedAttempts()).isEqualTo(1);
    }

    private RetreeverLoginGuardService.FailedLoginResult failTimes(
            RetreeverLoginGuardService.LoginGuardState initialState,
            int times) {
        RetreeverLoginGuardService.LoginGuardState state = initialState;
        RetreeverLoginGuardService.FailedLoginResult result = null;
        for (int i = 0; i < times; i++) {
            result = loginGuardService.recordFailedAttempt(state);
            state = result.state();
        }
        return result;
    }

    private RetreeverAuthProperties authProperties() {
        RetreeverAuthProperties authProperties = new RetreeverAuthProperties();
        authProperties.setUsername("admin");
        authProperties.setPassword("secret");
        authProperties.setSecret("123e4567-e89b-12d3-a456-426614174000");
        authProperties.afterPropertiesSet();
        return authProperties;
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
