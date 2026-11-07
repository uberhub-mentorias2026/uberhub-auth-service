package br.edu.iftm.uberhub_mentorias_auth_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.edu.iftm.uberhub_mentorias_auth_service.dto.*;
import br.edu.iftm.uberhub_mentorias_auth_service.exception.*;
import br.edu.iftm.uberhub_mentorias_auth_service.service.AuthService;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Isolated HTTP contract tests: deliberately not a test of Eliezer's security filter chain. */
class AuthControllerTest {
    private AuthService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new AuthExceptionHandler()).build();
    }

    @Test
    void registerReturnsCreatedAndOnlyUserId() throws Exception {
        when(service.register(new RegisterRequest("firebase-token"))).thenReturn(new RegisterResponse("local-id"));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"firebase-token\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"userId\":\"local-id\"}"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        verify(service).register(new RegisterRequest("firebase-token"));
    }

    @Test
    void extraPermissionFieldsCannotReachRegistrationService() throws Exception {
        when(service.register(new RegisterRequest("token"))).thenReturn(new RegisterResponse("local-id"));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"token\",\"permissions\":[\"ADMIN\"],\"role\":\"ADMIN\",\"firebaseUid\":\"forged\"}"))
                .andExpect(status().isCreated());
        verify(service).register(new RegisterRequest("token"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "refresh"})
    void tokenResponsesAreNotCacheable(String operation) throws Exception {
        var response = new TokenResponse("access", "refresh", "Bearer", 900);
        if (operation.equals("login")) when(service.login(new LoginRequest("token"))).thenReturn(response);
        else when(service.refresh(new RefreshRequest("token"))).thenReturn(response);
        String field = operation.equals("login") ? "idToken" : "refreshToken";
        mvc.perform(post("/auth/" + operation).contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + field + "\":\"token\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void recoveryReturnsMessage() throws Exception {
        when(service.recoverPassword(new RecoverPasswordRequest("user@example.com")))
                .thenReturn(new MessageResponse(AuthService.RECOVERY_REQUESTED_MESSAGE));
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.RECOVERY_REQUESTED_MESSAGE));
        verify(service).recoverPassword(new RecoverPasswordRequest("user@example.com"));
    }

    @ParameterizedTest
    @CsvSource({"register,idToken", "login,idToken", "refresh,refreshToken", "recover-password,email"})
    void missingAndBlankFieldsAreRejected(String operation, String field) throws Exception {
        for (String body : new String[]{"{}", "{\"" + field + "\":null}", "{\"" + field + "\":\"   \"}"}) {
            mvc.perform(post("/auth/" + operation).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidEmailDoesNotReachService() throws Exception {
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(not(containsString("not-an-email"))));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "login", "refresh", "recover-password"})
    void malformedJsonDoesNotLeakRequest(String operation) throws Exception {
        mvc.perform(post("/auth/" + operation).contentType(MediaType.APPLICATION_JSON)
                .content("{SECRET_TOKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(content().string(not(containsString("SECRET_TOKEN"))));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"INVALID_FIREBASE_TOKEN,401", "INVALID_REFRESH_TOKEN,401", "ACCOUNT_INACTIVE,403",
            "ACCOUNT_ALREADY_EXISTS,409", "REGISTRATION_REQUIRED,409", "DEPENDENCY_UNAVAILABLE,503",
            "INTERNAL_ERROR,500", "VALIDATION_ERROR,400", "MALFORMED_REQUEST,400"})
    void mapsKnownFailuresWithoutLeakingProviderDetails(AuthErrorCode code, int expectedStatus) throws Exception {
        when(service.login(any())).thenThrow(new AuthException(code, new RuntimeException("SECRET_PROVIDER_DETAIL")));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token\"}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.instance").value("/auth/login"))
                .andExpect(content().string(not(containsString("SECRET_PROVIDER_DETAIL"))));
    }

    @Test
    void unexpectedFailureReturnsGeneric500() throws Exception {
        when(service.register(any())).thenThrow(new IllegalStateException("SECRET_PROVIDER_DETAIL"));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("SECRET_PROVIDER_DETAIL"))));
    }

    @ParameterizedTest
    @CsvSource({"register,idToken", "login,idToken", "refresh,refreshToken", "recover-password,email"})
    void inputsAboveLimitAreRejectedBeforeService(String operation, String field) throws Exception {
        String oversized = "email".equals(field)
                ? "a".repeat(245) + "@example.com"
                : "a".repeat(InputLimits.TOKEN_MAX + 1);
        mvc.perform(post("/auth/" + operation).contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + field + "\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='" + field + "')]").exists())
                .andExpect(content().string(not(containsString(oversized))));
        verifyNoInteractions(service);
    }

    @Test
    void tokenAtLimitIsAccepted() throws Exception {
        String token = "a".repeat(InputLimits.TOKEN_MAX);
        when(service.login(any())).thenReturn(new TokenResponse("access", "refresh", "Bearer", 900));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + token + "\"}"))
                .andExpect(status().isOk());
        verify(service).login(new LoginRequest(token));
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "login", "refresh", "recover-password"})
    void getIsNotAllowedAndAdvertisesPost(String operation) throws Exception {
        mvc.perform(get("/auth/" + operation))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(content().string(""));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "login", "refresh", "recover-password"})
    void nonJsonBodyIsUnsupportedMediaType(String operation) throws Exception {
        mvc.perform(post("/auth/" + operation).contentType(MediaType.TEXT_PLAIN).content("plain-body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").doesNotExist());
        verifyNoInteractions(service);
    }
}
