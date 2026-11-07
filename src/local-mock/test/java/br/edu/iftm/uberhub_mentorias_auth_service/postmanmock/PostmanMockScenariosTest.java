package br.edu.iftm.uberhub_mentorias_auth_service.postmanmock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica, com o contexto real e o perfil postman-mock, que cada cenário da collection do Postman
 * produz o status e o código esperados. Usa apenas valores fictícios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(PostmanMockGuard.PROFILE)
class PostmanMockScenariosTest {
    private static final String GENERIC_MESSAGE =
            "Se houver uma conta elegível, enviaremos as instruções de recuperação.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void registerSucceedsForNewUser() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-new-user\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("fake-user-new-user"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void registerRejectsMissingIdToken() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerRejectsInvalidIdToken() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_FIREBASE_TOKEN"));
    }

    @Test
    void registerReportsDuplicateAccount() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-existing-user\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void loginSucceedsForExistingActiveAccountWithoutCaching() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-existing-user\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("fake-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginRequiresRegistrationForUnknownIdentity() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-new-user\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_REQUIRED"));
    }

    @Test
    void loginRejectsInactiveAccount() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-inactive-user\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void loginRejectsInvalidIdToken() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_FIREBASE_TOKEN"));
    }

    @Test
    void refreshRotatesValidToken() throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"mock-refresh-valid\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-rotated"));
    }

    @Test
    void refreshRejectsInactiveAccount() throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"mock-refresh-inactive\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void refreshRejectsInvalidToken() throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"mock-refresh-unknown\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void recoverPasswordReturnsGenericMessage() throws Exception {
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
    }

    @Test
    void recoverPasswordRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void recoverPasswordReportsSimulatedUnavailability() throws Exception {
        mvc.perform(post("/auth/recover-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unavailable@example.test\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(content().string(not(containsString(GENERIC_MESSAGE))));
    }

    @Test
    void routesOutsideTheFourPostEndpointsAreDenied() throws Exception {
        mvc.perform(get("/auth/login")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/unknown").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noDefaultUserIsCreatedSoNoPasswordIsPrinted() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void responsesDoNotExposeProviderDetailsOrStackTraces() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"mock-invalid\"}"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.edu"))));
    }
}
