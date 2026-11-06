package br.edu.iftm.uberhub_mentorias_auth_service.port;

import br.edu.iftm.uberhub_mentorias_auth_service.model.VerifiedIdentity;

/**
 * Arthur's integration: verify signature, expiry and Firebase project before returning identity.
 * Apply the agreed revocation policy. Never merely decode a JWT.
 * Signal INVALID_FIREBASE_TOKEN or DEPENDENCY_UNAVAILABLE with AuthException.
 */
public interface FirebaseIdentityVerifier {
    VerifiedIdentity verify(String idToken);
}
