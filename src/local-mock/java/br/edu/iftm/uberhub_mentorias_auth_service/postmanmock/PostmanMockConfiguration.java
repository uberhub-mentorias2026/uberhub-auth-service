package br.edu.iftm.uberhub_mentorias_auth_service.postmanmock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import br.edu.iftm.uberhub_mentorias_auth_service.port.FirebaseIdentityVerifier;
import br.edu.iftm.uberhub_mentorias_auth_service.port.PasswordRecoveryGateway;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UberHubSessionService;
import br.edu.iftm.uberhub_mentorias_auth_service.port.UserAccountPort;

/**
 * Ativo somente com o perfil "postman-mock". Fora desse perfil nenhum destes beans existe e a
 * configuração de segurança padrão da aplicação permanece inalterada.
 */
@Configuration
@Profile(PostmanMockGuard.PROFILE)
@EnableWebSecurity
public class PostmanMockConfiguration {

    public PostmanMockConfiguration(Environment environment) {
        PostmanMockGuard.verify(environment);
    }

    /**
     * Sem usuários: impede que o Spring gere e imprima uma senha padrão no log durante os testes manuais.
     * Nenhuma credencial é criada neste perfil.
     */
    @Bean
    UserDetailsService postmanMockUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    FirebaseIdentityVerifier postmanMockFirebaseIdentityVerifier() {
        return new PostmanMockPorts.FakeFirebaseIdentityVerifier();
    }

    @Bean
    UserAccountPort postmanMockUserAccountPort() {
        return new PostmanMockPorts.FakeUserAccountPort();
    }

    @Bean
    UberHubSessionService postmanMockUberHubSessionService() {
        return new PostmanMockPorts.FakeUberHubSessionService();
    }

    @Bean
    PasswordRecoveryGateway postmanMockPasswordRecoveryGateway() {
        return new PostmanMockPorts.FakePasswordRecoveryGateway();
    }

    /** Libera somente as quatro rotas POST de autenticação; qualquer outra requisição é negada. */
    @Bean
    SecurityFilterChain postmanMockSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register", "/auth/login", "/auth/refresh", "/auth/recover-password")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
