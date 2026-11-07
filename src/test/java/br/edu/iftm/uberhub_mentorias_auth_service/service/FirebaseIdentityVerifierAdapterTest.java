package br.edu.iftm.uberhub_mentorias_auth_service.service;

import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthErrorCode;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthException;
import br.edu.iftm.uberhub_mentorias_auth_service.model.VerifiedIdentity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Firebase token verification behavior without external credentials.
 *
 * @author Arthur Cavich
 */
class FirebaseIdentityVerifierAdapterTest {
    private FirebaseAuth firebaseAuth;
    private FirebaseIdentityVerifierAdapter verifier;

    @BeforeEach
    void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        verifier = new FirebaseIdentityVerifierAdapter(firebaseAuth);
    }

    @Test
    void rejectsBlankToken() {
        AuthException exception = assertThrows(AuthException.class, () -> verifier.verify(" "));

        assertEquals(AuthErrorCode.INVALID_FIREBASE_TOKEN, exception.code());
    }

    @Test
    void returnsTrustedIdentityFromVerifiedToken() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
        when(token.getUid()).thenReturn("firebase-user-1");
        when(token.getEmail()).thenReturn("user@example.com");
        when(token.isEmailVerified()).thenReturn(true);

        VerifiedIdentity identity = verifier.verify("id-token");

        assertEquals(new VerifiedIdentity("firebase-user-1", "user@example.com", true), identity);
        verify(firebaseAuth).verifyIdToken("id-token", true);
    }

    @Test
    void mapsUnexpectedFirebaseStateToDependencyUnavailable() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenThrow(new IllegalStateException("Firebase is not available"));

        AuthException exception = assertThrows(AuthException.class, () -> verifier.verify("id-token"));

        assertEquals(AuthErrorCode.DEPENDENCY_UNAVAILABLE, exception.code());
    }
}
