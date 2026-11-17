package br.edu.iftm.uberhub_mentorias_auth_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credenciais")
public class Credencial {

    @Id
    private String id;

    @Field("firebase_uid")
    private String firebaseUid;

    private String email;


}
