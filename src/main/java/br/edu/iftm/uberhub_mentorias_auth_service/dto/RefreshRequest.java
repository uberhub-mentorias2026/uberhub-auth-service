package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Campo obrigatório.") String refreshToken) {
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=REDACTED]";
    }
}
