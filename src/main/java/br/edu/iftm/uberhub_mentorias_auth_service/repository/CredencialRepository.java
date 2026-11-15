package br.edu.iftm.uberhub_mentorias_auth_service.repository;

import br.edu.iftm.uberhub_mentorias_auth_service.model.Credencial;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialRepository extends MongoRepository<Credencial, String> {

    Optional<Credencial> findByFirebaseUid(String firebaseUid);
}