package br.edu.iftm.uberhub_mentorias_auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.iftm.uberhub_mentorias_auth_service.service.AuthService;
import br.edu.iftm.uberhub_mentorias_auth_service.dto.RegisterRequest;
import br.edu.iftm.uberhub_mentorias_auth_service.model.*;
import java.util.Optional;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import br.edu.iftm.uberhub_mentorias_auth_service.repository.CredencialRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.edu.iftm.uberhub_mentorias_auth_service.port.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration")
class UberhubMentoriasAuthServiceApplicationTests {
    @MockitoBean CredencialRepository credencialRepository;
    @MockitoBean FirebaseIdentityVerifier identities;
    @MockitoBean UserAccountPort users;
    @MockitoBean UberHubSessionService sessions;
    @MockitoBean PasswordRecoveryGateway recovery;

    @Autowired AuthService service;

    @Test
    void springInjectsRepositoryAndRegistrationPersistsVerifiedIdentity() {
        var identity = new VerifiedIdentity("verified-uid", "verified@example.test", true);
        when(identities.verify("test-token")).thenReturn(identity);
        when(users.register(identity)).thenReturn(new UserAccount("account-id", "verified-uid", true, Set.of()));
        when(credencialRepository.findByFirebaseUid("verified-uid")).thenReturn(Optional.empty());

        assertThat(service.register(new RegisterRequest("test-token")).userId()).isEqualTo("account-id");
        verify(credencialRepository).save(argThat(credential ->
                "verified-uid".equals(credential.getFirebaseUid())
                && "verified@example.test".equals(credential.getEmail())));
    }

    @Test
    void existingCredentialIsNotSavedAgain() {
        var identity = new VerifiedIdentity("existing-uid", "existing@example.test", true);
        when(identities.verify("existing-token")).thenReturn(identity);
        when(users.register(identity)).thenReturn(new UserAccount("account-id", "existing-uid", true, Set.of()));
        when(credencialRepository.findByFirebaseUid("existing-uid"))
                .thenReturn(Optional.of(Credencial.builder().firebaseUid("existing-uid").build()));

        service.register(new RegisterRequest("existing-token"));
        verify(credencialRepository, never()).save(any());
    }

    @Test
    void contextLoads() {
    }
}
