package br.edu.iftm.uberhub_mentorias_auth_service.dto;

/**
 * Limites de tamanho das entradas da API. Mantidos em um único lugar e refletidos em docs/auth-api.yaml.
 *
 * TOKEN_MAX: 8192 caracteres. Tokens JWT (ID Token do Firebase e refresh token do UberHub) costumam ficar
 * bem abaixo disso. O limite é conservador, para não rejeitar tokens legítimos, e não substitui a validação
 * criptográfica feita pelos componentes responsáveis. Revisar com Arthur (Firebase) e Eliezer (sessão) antes do merge.
 *
 * EMAIL_MAX: 254 caracteres, limite prático do endereço de e-mail (RFC 5321).
 */
public final class InputLimits {
    public static final int TOKEN_MAX = 8192;
    public static final int EMAIL_MAX = 254;

    private InputLimits() {
    }
}
