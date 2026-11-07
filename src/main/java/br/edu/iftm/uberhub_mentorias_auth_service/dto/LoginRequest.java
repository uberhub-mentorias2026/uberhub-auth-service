package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Campo obrigatório.")
        @Size(max = InputLimits.TOKEN_MAX, message = "Tamanho máximo excedido.") String idToken) {
    @Override
    public String toString() {
        return "LoginRequest[idToken=REDACTED]";
    }
}
