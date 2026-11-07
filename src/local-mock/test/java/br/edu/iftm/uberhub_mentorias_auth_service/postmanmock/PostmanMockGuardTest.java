package br.edu.iftm.uberhub_mentorias_auth_service.postmanmock;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class PostmanMockGuardTest {

    private static MockEnvironment localMockEnvironment() {
        return new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("firebase.enabled", "false");
    }

    @Test
    void acceptsLoopbackAddressWithFirebaseDisabled() {
        MockEnvironment env = localMockEnvironment();
        env.setActiveProfiles(PostmanMockGuard.PROFILE);
        assertThatCode(() -> PostmanMockGuard.verify(env)).doesNotThrowAnyException();
    }

    @Test
    void refusesNonLoopbackAddress() {
        MockEnvironment env = localMockEnvironment().withProperty("server.address", "0.0.0.0");
        env.setActiveProfiles(PostmanMockGuard.PROFILE);
        assertThatThrownBy(() -> PostmanMockGuard.verify(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("127.0.0.1");
    }

    @Test
    void refusesMissingAddress() {
        MockEnvironment env = new MockEnvironment().withProperty("firebase.enabled", "false");
        env.setActiveProfiles(PostmanMockGuard.PROFILE);
        assertThatThrownBy(() -> PostmanMockGuard.verify(env)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesProductionProfiles() {
        for (String forbidden : new String[]{"prod", "production"}) {
            MockEnvironment env = localMockEnvironment();
            env.setActiveProfiles(PostmanMockGuard.PROFILE, forbidden);
            assertThatThrownBy(() -> PostmanMockGuard.verify(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(forbidden);
        }
    }

    @Test
    void refusesEnabledFirebase() {
        MockEnvironment env = localMockEnvironment().withProperty("firebase.enabled", "true");
        env.setActiveProfiles(PostmanMockGuard.PROFILE);
        assertThatThrownBy(() -> PostmanMockGuard.verify(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("firebase.enabled");
    }

    @Test
    void doesNothingWhenProfileIsNotActive() {
        MockEnvironment env = new MockEnvironment().withProperty("server.address", "0.0.0.0");
        env.setActiveProfiles("default");
        assertThatCode(() -> PostmanMockGuard.verify(env)).doesNotThrowAnyException();
    }
}
