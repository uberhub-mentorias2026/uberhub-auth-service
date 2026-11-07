package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverPasswordRequest(
        @NotBlank(message = "Campo obrigatório.")
        @Size(max = InputLimits.EMAIL_MAX, message = "Tamanho máximo excedido.")
        @Email(message = "E-mail inválido.") String email) {
    @Override
    public String toString() {
        return "RecoverPasswordRequest[email=REDACTED]";
    }
}
