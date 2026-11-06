package br.edu.iftm.uberhub_mentorias_auth_service.port;

import java.util.Optional;
import br.edu.iftm.uberhub_mentorias_auth_service.model.UserAccount;
import br.edu.iftm.uberhub_mentorias_auth_service.model.VerifiedIdentity;

/** Integration with the owner of user data; no database technology is assumed. */
public interface UserAccountPort {
    /**
     * Atomically create an account unique by Firebase UID, or signal ACCOUNT_ALREADY_EXISTS.
     * Create with NO permissions: never grant ADMIN or choose mentor/mentee here.
     * Do not merge accounts by email. Signal DEPENDENCY_UNAVAILABLE for operational failures.
     */
    UserAccount register(VerifiedIdentity identity);

    /** An absent account is Optional.empty(), not a provider error. */
    Optional<UserAccount> findByFirebaseUid(String firebaseUid);
}
