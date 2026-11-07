package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "Campo obrigatório.")
        @Size(max = InputLimits.TOKEN_MAX, message = "Tamanho máximo excedido.") String refreshToken) {
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=REDACTED]";
    }
}
