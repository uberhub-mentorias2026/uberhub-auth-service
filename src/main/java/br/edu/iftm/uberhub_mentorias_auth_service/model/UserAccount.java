package br.edu.iftm.uberhub_mentorias_auth_service.model;

import java.util.Set;

/** Authentication view, not a database entity. Permissions originate from the account owner. */
public record UserAccount(String userId, String firebaseUid, boolean active, Set<String> permissions) {
    public UserAccount {
        permissions = Set.copyOf(permissions);
    }
}
