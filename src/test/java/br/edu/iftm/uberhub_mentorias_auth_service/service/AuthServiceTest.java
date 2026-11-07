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
    @ValueSource(strings = {"register", "login"})
    void invalidIdentityStopsProcessing(String operation) {
        var failure = new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        when(identities.verify("bad-token")).thenThrow(failure);
        assertThatThrownBy(() -> {
            if (operation.equals("register")) service.register(new RegisterRequest("bad-token"));
            else service.login(new LoginRequest("bad-token"));
        }).isSameAs(failure);
        verifyNoInteractions(users, sessions, recovery);
    }

    @ParameterizedTest
    @ValueSource(strings = {"google-id-token", "email-password-id-token"})
    void loginUsesFirebaseUidAndExistingAccountPermissions(String token) {
        var existing = new UserAccount("local-id", "firebase-uid", true, Set.of("EXISTING_PERMISSION"));
        when(identities.verify(token)).thenReturn(identity);
        when(users.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(existing));
        when(sessions.issue(existing)).thenReturn(tokens);
        assertThat(service.login(new LoginRequest(token))).isEqualTo(TokenResponse.from(tokens));
        verify(users, never()).register(any());
        verify(sessions).issue(existing);
    }

    @Test
    void firstGoogleAccessRequiresRegistrationThenAllowsLogin() {
        when(identities.verify("google-token")).thenReturn(identity);
        when(users.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty(), Optional.of(account));
        assertThatThrownBy(() -> service.login(new LoginRequest("google-token")))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo(AuthErrorCode.REGISTRATION_REQUIRED));
        verifyNoInteractions(sessions);
        verify(users, never()).register(any());
        when(users.register(identity)).thenReturn(account);
        assertThat(service.register(new RegisterRequest("google-token")).userId()).isEqualTo("local-id");
        when(sessions.issue(account)).thenReturn(tokens);
        assertThat(service.login(new LoginRequest("google-token"))).isEqualTo(TokenResponse.from(tokens));
    }

    @Test
    void inactiveAccountCannotReceiveTokens() {
        when(identities.verify("token")).thenReturn(identity);
        when(users.findByFirebaseUid("firebase-uid"))
                .thenReturn(Optional.of(new UserAccount("local-id", "firebase-uid", false, Set.of())));
        assertThatThrownBy(() -> service.login(new LoginRequest("token")))
                .isInstanceOfSatisfying(AuthException.class,
                        error -> assertThat(error.code()).isEqualTo(AuthErrorCode.ACCOUNT_INACTIVE));
        verifyNoInteractions(sessions);
    }

    @Test
    void refreshDelegatesAtomicRenewalAndReturnsRotatedToken() {
        when(sessions.refresh("old-refresh")).thenReturn(tokens);
        assertThat(service.refresh(new RefreshRequest("old-refresh"))).isEqualTo(TokenResponse.from(tokens));
        verifyNoInteractions(identities, users, recovery);
    }

    @Test
    void invalidRefreshPropagatesWithoutIssuingAnotherSession() {
        var failure = new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        when(sessions.refresh("expired")).thenThrow(failure);
        assertThatThrownBy(() -> service.refresh(new RefreshRequest("expired"))).isSameAs(failure);
        verify(sessions, never()).issue(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"eligible@example.com", "unknown@example.com", "google-only@example.com"})
    void recoveryAlwaysReturnsGenericMessage(String email) {
        assertThat(service.recoverPassword(new RecoverPasswordRequest(email)).message())
                .isEqualTo("Se houver uma conta elegível, enviaremos as instruções de recuperação.");
        verify(recovery).requestRecovery(email);
        verifyNoInteractions(identities, users, sessions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"firebase", "register", "lookup", "issue", "refresh", "recovery"})
    void operationalFailuresAreNotConvertedIntoSuccess(String dependency) {
        var failure = new AuthException(AuthErrorCode.DEPENDENCY_UNAVAILABLE);
        switch (dependency) {
            case "firebase" -> {
                when(identities.verify("token")).thenThrow(failure);
                assertThatThrownBy(() -> service.login(new LoginRequest("token"))).isSameAs(failure);
                verifyNoInteractions(users, sessions);
            }
            case "register" -> {
                when(identities.verify("token")).thenReturn(identity);
                when(users.register(identity)).thenThrow(failure);
                assertThatThrownBy(() -> service.register(new RegisterRequest("token"))).isSameAs(failure);
                verifyNoInteractions(sessions);
            }
            case "lookup" -> {
                when(identities.verify("token")).thenReturn(identity);
                when(users.findByFirebaseUid("firebase-uid")).thenThrow(failure);
                assertThatThrownBy(() -> service.login(new LoginRequest("token"))).isSameAs(failure);
                verifyNoInteractions(sessions);
            }
            case "issue" -> {
                when(identities.verify("token")).thenReturn(identity);
                when(users.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(account));
                when(sessions.issue(account)).thenThrow(failure);
                assertThatThrownBy(() -> service.login(new LoginRequest("token"))).isSameAs(failure);
            }
            case "refresh" -> {
                when(sessions.refresh("token")).thenThrow(failure);
                assertThatThrownBy(() -> service.refresh(new RefreshRequest("token"))).isSameAs(failure);
            }
            case "recovery" -> {
                doThrow(failure).when(recovery).requestRecovery("user@example.com");
                assertThatThrownBy(() -> service.recoverPassword(new RecoverPasswordRequest("user@example.com")))
                        .isSameAs(failure);
            }
        }
    }

    @Test
    void tokenObjectsDoNotLeakTokensThroughToString() {
        assertThat(new RegisterRequest("secret").toString()).doesNotContain("secret");
        assertThat(new LoginRequest("secret").toString()).doesNotContain("secret");
        assertThat(new RefreshRequest("secret").toString()).doesNotContain("secret");
        assertThat(tokens.toString()).doesNotContain("rotated-refresh");
        assertThat(TokenResponse.from(tokens).toString()).doesNotContain("rotated-refresh");
    }
}
