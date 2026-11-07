package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import br.edu.iftm.uberhub_mentorias_auth_service.model.TokenPair;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    public static TokenResponse from(TokenPair tokens) {
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer", tokens.expiresIn());
    }

    @Override
    public String toString() {
        return "TokenResponse[tokens=REDACTED]";
    }
}
