package br.edu.iftm.uberhub_mentorias_auth_service.exception;

/** Stable integration error. Provider details must never become a public response. */
public class AuthException extends RuntimeException {
    private final AuthErrorCode code;

    public AuthException(AuthErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public AuthException(AuthErrorCode code, Throwable cause) {
        super(code.name(), cause);
        this.code = code;
    }

    public AuthErrorCode code() {
        return code;
    }
}
