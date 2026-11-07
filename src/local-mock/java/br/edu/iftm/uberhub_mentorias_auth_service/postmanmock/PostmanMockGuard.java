package br.edu.iftm.uberhub_mentorias_auth_service.postmanmock;

import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;

/**
 * Impede que o perfil de testes manuais seja usado fora do ambiente local.
 * Só atua quando o perfil "postman-mock" está ativo; caso contrário não faz nada.
 */
public final class PostmanMockGuard {
    public static final String PROFILE = "postman-mock";
    static final String LOOPBACK_ADDRESS = "127.0.0.1";
    private static final List<String> FORBIDDEN_PROFILES = List.of("prod", "production");

    private PostmanMockGuard() {
    }

    public static void verify(Environment environment) {
        List<String> active = Arrays.asList(environment.getActiveProfiles());
        if (!active.contains(PROFILE)) {
            return;
        }
        for (String forbidden : FORBIDDEN_PROFILES) {
            if (active.contains(forbidden)) {
                throw new IllegalStateException("O perfil " + PROFILE + " não pode ser usado com o perfil " + forbidden + ".");
            }
        }
        if (!LOOPBACK_ADDRESS.equals(environment.getProperty("server.address"))) {
            throw new IllegalStateException("O perfil " + PROFILE + " exige server.address=" + LOOPBACK_ADDRESS + ".");
        }
        if (environment.getProperty("firebase.enabled", Boolean.class, false)) {
            throw new IllegalStateException("O perfil " + PROFILE + " não aceita firebase.enabled=true.");
        }
    }
}
