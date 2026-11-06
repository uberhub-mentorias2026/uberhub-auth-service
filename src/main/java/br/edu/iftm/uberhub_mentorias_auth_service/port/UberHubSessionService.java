package br.edu.iftm.uberhub_mentorias_auth_service.port;

import br.edu.iftm.uberhub_mentorias_auth_service.model.TokenPair;
import br.edu.iftm.uberhub_mentorias_auth_service.model.UserAccount;

/** Eliezer's session implementation. No cryptographic implementation belongs in AuthService. */
public interface UberHubSessionService {
    TokenPair issue(UserAccount account);

    /**
     * Validate refresh credentials, account status, revocation and reuse, and renew atomically.
     * Return the refresh token the client must keep (rotated if that policy is enabled).
     * Signal INVALID_REFRESH_TOKEN, ACCOUNT_INACTIVE or DEPENDENCY_UNAVAILABLE.
     */
    TokenPair refresh(String refreshToken);
}
