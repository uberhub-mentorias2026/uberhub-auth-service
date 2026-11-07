package br.edu.iftm.uberhub_mentorias_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverPasswordRequest(
        @NotBlank(message = "Campo obrigatório.")
        @Email(message = "E-mail inválido.") String email) {
}
