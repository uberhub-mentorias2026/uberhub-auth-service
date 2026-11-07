package br.edu.iftm.uberhub_mentorias_auth_service.service;

import org.springframework.stereotype.Service;
import br.edu.iftm.uberhub_mentorias_auth_service.dto.*;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthErrorCode;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthException;
import br.edu.iftm.uberhub_mentorias_auth_service.port.*;

@Service
public class AuthService {
    private final FirebaseIdentityVerifier identities;
    private final UserAccountPort users;
    private final UberHubSessionService sessions;
    private final PasswordRecoveryGateway recovery;

    public AuthService(FirebaseIdentityVerifier identities, UserAccountPort users,
                       UberHubSessionService sessions, PasswordRecoveryGateway recovery) {
        this.identities = identities;
        this.users = users;
        this.sessions = sessions;
        this.recovery = recovery;
    }

    public RegisterResponse register(RegisterRequest request) {
        var identity = identities.verify(request.idToken());
        var account = users.register(identity);
        return new RegisterResponse(account.userId());
    }

    public TokenResponse login(LoginRequest request) {
        var identity = identities.verify(request.idToken());
        var account = users.findByFirebaseUid(identity.firebaseUid())
                .orElseThrow(() -> new AuthException(AuthErrorCode.REGISTRATION_REQUIRED));
        if (!account.active()) {
            throw new AuthException(AuthErrorCode.ACCOUNT_INACTIVE);
        }
        return TokenResponse.from(sessions.issue(account));
    }


}
