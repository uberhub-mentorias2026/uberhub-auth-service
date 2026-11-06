package br.edu.iftm.uberhub_mentorias_auth_service.model;

/** Trusted result of Firebase verification. Email may be absent; never supplied by the caller. */
public record VerifiedIdentity(String firebaseUid, String email, boolean emailVerified) {
}
