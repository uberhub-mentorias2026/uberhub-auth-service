package br.edu.iftm.uberhub_mentorias_auth_service.model;

/** expiresIn is the access token lifetime in seconds. */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    @Override
    public String toString() {
        return "TokenPair[tokens=REDACTED]";
    }
}
