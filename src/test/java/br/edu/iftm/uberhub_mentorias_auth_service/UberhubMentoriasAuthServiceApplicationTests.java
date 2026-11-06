package br.edu.iftm.uberhub_mentorias_auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.edu.iftm.uberhub_mentorias_auth_service.port.*;

@SpringBootTest
class UberhubMentoriasAuthServiceApplicationTests {
    @MockitoBean FirebaseIdentityVerifier identities;
    @MockitoBean UserAccountPort users;
    @MockitoBean UberHubSessionService sessions;
    @MockitoBean PasswordRecoveryGateway recovery;

    @Test
    void contextLoads() {
    }
}
