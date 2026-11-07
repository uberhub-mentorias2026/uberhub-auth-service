package br.edu.iftm.uberhub_mentorias_auth_service.postmanmock;

import java.util.Optional;
import java.util.Set;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthErrorCode;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthException;
import br.edu.iftm.uberhub_mentorias_auth_service.model.TokenPair;
import br.edu.iftm.uberhub_mentorias_auth_service.model.UserAccount;
import br.edu.iftm.uberhub_mentorias_auth_service.model.VerifiedIdentity;
import br.edu.iftm.uberhub_mentorias_auth_service.port.FirebaseIdentityVerifier;
import br.edu.iftm.uberhub_mentorias_auth_service.port.PasswordRecoveryGateway;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UberHubSessionService;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UserAccountPort;

/**
 * Implementações fictícias e determinísticas das interfaces externas, usadas somente pelo perfil "postman-mock".
 *
 * Cenários selecionados pelo valor fictício enviado (sem estado entre requisições):
 *   idToken  "mock-new-user"       -> identidade sem cadastro (login: 409 REGISTRATION_REQUIRED; cadastro: 201)
 *   idToken  "mock-existing-user"  -> conta ativa (login: 200; cadastro: 409 ACCOUNT_ALREADY_EXISTS)
 *   idToken  "mock-inactive-user"  -> conta inativa (login: 403 ACCOUNT_INACTIVE)
 *   qualquer outro idToken         -> 401 INVALID_FIREBASE_TOKEN
 *   refreshToken "mock-refresh-valid"    -> 200 com rotação
 *   refreshToken "mock-refresh-inactive" -> 403 ACCOUNT_INACTIVE
 *   qualquer outro refreshToken          -> 401 INVALID_REFRESH_TOKEN
 *   e-mail "unavailable@example.test" -> 503 DEPENDENCY_UNAVAILABLE (recuperação)
 *
 * Nenhum valor aqui é credencial real. Tokens de saída têm o prefixo "fake-" ou "mock-".
 */
final class PostmanMockPorts {
    static final String NEW_USER_TOKEN = "mock-new-user";
    static final String EXISTING_USER_TOKEN = "mock-existing-user";
    static final String INACTIVE_USER_TOKEN = "mock-inactive-user";
    static final String REFRESH_VALID = "mock-refresh-valid";
    static final String REFRESH_INACTIVE = "mock-refresh-inactive";
    static final String UNAVAILABLE_EMAIL = "unavailable@example.test";

    private static final String NEW_UID = "fake-uid-new-user";
    private static final String EXISTING_UID = "fake-uid-existing-user";
    private static final String INACTIVE_UID = "fake-uid-inactive-user";

    private PostmanMockPorts() {
    }

    static final class FakeFirebaseIdentityVerifier implements FirebaseIdentityVerifier {
        @Override
        public VerifiedIdentity verify(String idToken) {
            if (NEW_USER_TOKEN.equals(idToken)) {
                return new VerifiedIdentity(NEW_UID, "new-user@example.test", true);
            }
            if (EXISTING_USER_TOKEN.equals(idToken)) {
                return new VerifiedIdentity(EXISTING_UID, "existing-user@example.test", true);
            }
            if (INACTIVE_USER_TOKEN.equals(idToken)) {
                return new VerifiedIdentity(INACTIVE_UID, "inactive-user@example.test", true);
            }
            throw new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        }
    }

    static final class FakeUserAccountPort implements UserAccountPort {
        @Override
        public UserAccount register(VerifiedIdentity identity) {
            if (NEW_UID.equals(identity.firebaseUid())) {
                return new UserAccount("fake-user-new-user", NEW_UID, true, Set.of());
            }
            throw new AuthException(AuthErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        @Override
        public Optional<UserAccount> findByFirebaseUid(String firebaseUid) {
            if (EXISTING_UID.equals(firebaseUid)) {
                return Optional.of(new UserAccount("fake-user-existing-user", EXISTING_UID, true, Set.of()));
            }
            if (INACTIVE_UID.equals(firebaseUid)) {
                return Optional.of(new UserAccount("fake-user-inactive-user", INACTIVE_UID, false, Set.of()));
            }
            return Optional.empty();
        }
    }

    static final class FakeUberHubSessionService implements UberHubSessionService {
        @Override
        public TokenPair issue(UserAccount account) {
            return new TokenPair("fake-access-token", REFRESH_VALID, 900);
        }

        @Override
        public TokenPair refresh(String refreshToken) {
            if (REFRESH_VALID.equals(refreshToken)) {
                return new TokenPair("fake-access-token", "mock-refresh-rotated", 900);
            }
            if (REFRESH_INACTIVE.equals(refreshToken)) {
                throw new AuthException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    static final class FakePasswordRecoveryGateway implements PasswordRecoveryGateway {
        @Override
        public void requestRecovery(String email) {
            if (UNAVAILABLE_EMAIL.equals(email)) {
                throw new AuthException(AuthErrorCode.DEPENDENCY_UNAVAILABLE);
            }
        }
    }
}
