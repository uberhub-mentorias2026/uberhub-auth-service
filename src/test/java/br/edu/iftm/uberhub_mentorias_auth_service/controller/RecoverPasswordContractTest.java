package br.edu.iftm.uberhub_mentorias_auth_service.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthErrorCode;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthException;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.AuthExceptionHandler;
import br.edu.iftm.uberhub_mentorias_auth_service.port.FirebaseIdentityVerifier;
import br.edu.iftm.uberhub_mentorias_auth_service.port.PasswordRecoveryGateway;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UberHubSessionService;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UserAccountPort;
import br.edu.iftm.uberhub_mentorias_auth_service.service.AuthService;

/**
 * Contrato de recuperação de senha com o serviço real e portas mockadas.
 * Verifica a mensagem exata (anti-enumeração) sem depender de mock de resposta.
 */
class RecoverPasswordContractTest {
    private PasswordRecoveryGateway recovery;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recovery = mock(PasswordRecoveryGateway.class);
        AuthService realService = new AuthService(
                mock(FirebaseIdentityVerifier.class),
                mock(UserAccountPort.class),
                mock(UberHubSessionService.class),
                recovery);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(realService))
                .setControllerAdvice(new AuthExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"eligible@example.com", "unknown@example.com", "google-only@example.com"})
    void responseIsExactAndIdenticalForAnyEmail(String email) throws Exception {
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"message\":\"Se houver uma conta elegível, enviaremos as instruções de recuperação.\"}",
                        true));
    }

    @Test
    void operationalFailureIsReportedAsUnavailableWithoutGenericSuccessMessage() throws Exception {
        doThrow(new AuthException(AuthErrorCode.DEPENDENCY_UNAVAILABLE))
                .when(recovery).requestRecovery("user@example.com");
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("instruções de recuperação"))));
    }
}
