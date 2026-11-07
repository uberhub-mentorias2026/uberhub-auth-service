package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank(message = "Campo obrigatório.") String idToken) {
    @Override
    public String toString() {
        return "RegisterRequest[idToken=REDACTED]";
    }
}
