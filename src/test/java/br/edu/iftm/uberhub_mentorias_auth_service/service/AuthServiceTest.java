package br.edu.iftm.uberhub_mentorias_auth_service.service;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.edu.iftm.uberhub_mentorias_auth_service.dto.*;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.*;
import br.edu.iftm.uberhub_mentorias_auth_service.model.*;
import br.edu.iftm.uberhub_mentorias_auth_service.port.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock FirebaseIdentityVerifier identities;
    @Mock UserAccountPort users;
    @Mock UberHubSessionService sessions;
    @Mock PasswordRecoveryGateway recovery;
    @InjectMocks AuthService service;

    private final VerifiedIdentity identity = new VerifiedIdentity("firebase-uid", "user@example.com", true);
    private final UserAccount account = new UserAccount("local-id", "firebase-uid", true, Set.of());
    private final TokenPair tokens = new TokenPair("access", "rotated-refresh", 900);

    @ParameterizedTest
    @ValueSource(strings = {"google-id-token", "email-password-id-token"})
    void registersVerifiedIdentityWithoutAssigningPermissionsOrIssuingTokens(String token) {
        when(identities.verify(token)).thenReturn(identity);
        when(users.register(identity)).thenReturn(account);
        assertThat(service.register(new RegisterRequest(token)).userId()).isEqualTo("local-id");
        var order = inOrder(identities, users);
        order.verify(identities).verify(token);
        order.verify(users).register(identity);
        verifyNoMoreInteractions(users);
        verifyNoInteractions(sessions, recovery);
    }

    @Test
    void duplicateRegistrationIsNotConvertedIntoLogin() {
        when(identities.verify("id-token")).thenReturn(identity);
        var failure = new AuthException(AuthErrorCode.ACCOUNT_ALREADY_EXISTS);
        when(users.register(identity)).thenThrow(failure);
        assertThatThrownBy(() -> service.register(new RegisterRequest("id-token"))).isSameAs(failure);
        verifyNoInteractions(sessions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"register"})
    void invalidIdentityStopsProcessing(String operation) {
        var failure = new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        when(identities.verify("bad-token")).thenThrow(failure);
        assertThatThrownBy(() -> {
            if (operation.equals("register")) service.register(new RegisterRequest("bad-token"));
        }).isSameAs(failure);
        verifyNoInteractions(users, sessions, recovery);
    }







    @ParameterizedTest
    @ValueSource(strings = {"firebase", "register"})
    void operationalFailuresAreNotConvertedIntoSuccess(String dependency) {
        var failure = new AuthException(AuthErrorCode.DEPENDENCY_UNAVAILABLE);
        switch (dependency) {
            case "firebase" -> {
                when(identities.verify("token")).thenThrow(failure);
                assertThatThrownBy(() -> service.register(new RegisterRequest("token"))).isSameAs(failure);
                verifyNoInteractions(users, sessions);
            }
            case "register" -> {
                when(identities.verify("token")).thenReturn(identity);
                when(users.register(identity)).thenThrow(failure);
                assertThatThrownBy(() -> service.register(new RegisterRequest("token"))).isSameAs(failure);
                verifyNoInteractions(sessions);
            }
        }
    }

    @Test
    void tokenObjectsDoNotLeakTokensThroughToString() {
        assertThat(new RegisterRequest("secret").toString()).doesNotContain("secret");
        assertThat(tokens.toString()).doesNotContain("rotated-refresh");
    }
}
