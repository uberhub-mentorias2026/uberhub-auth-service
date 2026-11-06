package br.edu.iftm.uberhub_mentorias_auth_service.port;

/** Firebase recovery integration, including delivery rather than only link generation. */
public interface PasswordRecoveryGateway {
    /**
     * Return normally for unknown or ineligible accounts to avoid account enumeration.
     * Do not create passwords for Google-only accounts or return reset links.
     * Signal DEPENDENCY_UNAVAILABLE for operational failures.
     */
    void requestRecovery(String email);
}
