package br.edu.iftm.uberhub_mentorias_auth_service.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthErrorCode;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthException;
import br.edu.iftm.uberhub_mentorias_auth_service.model.VerifiedIdentity;
import br.edu.iftm.uberhub_mentorias_auth_service.port.FirebaseIdentityVerifier;

/**
 * Validates Firebase ID tokens and exposes the trusted identity.
 *
 * @author Arthur Cavich
 */
@Component
@ConditionalOnBean(FirebaseAuth.class)
public class FirebaseIdentityVerifierAdapter implements FirebaseIdentityVerifier {
    private final FirebaseAuth firebaseAuth;

    public FirebaseIdentityVerifierAdapter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public VerifiedIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        }

        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            return new VerifiedIdentity(
                    token.getUid(),
                    token.getEmail(),
                    Boolean.TRUE.equals(token.isEmailVerified()));
        } catch (FirebaseAuthException exception) {
            throw new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN, exception);
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN, exception);
        } catch (IllegalStateException exception) {
            throw new AuthException(AuthErrorCode.DEPENDENCY_UNAVAILABLE, exception);
        }
    }
}
