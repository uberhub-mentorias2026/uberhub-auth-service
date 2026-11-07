# Base dos contratos de autenticação

Esta entrega contém interfaces, modelos, erros compartilhados e a integração Firebase Admin desenvolvida por Arthur Cavich. Não contém controllers, DTOs HTTP, serviços de coordenação nem endpoints de cadastro, login, renovação ou recuperação. Não implementa banco, JWT, envio de e-mail ou filtros de segurança. O Profile Service permanece intacto.

## Responsabilidades e contratos

- Arthur: implementação original de `FirebaseIdentityVerifier` incorporada por meio de `FirebaseIdentityVerifierAdapter`, validando assinatura, expiração, projeto Firebase e a política acordada de revogação antes de retornar `VerifiedIdentity`. Google e e-mail/senha utilizam o mesmo contrato. Não basta decodificar o token.
- Illana/Eliezer: implementar `UserAccountPort`. A operação `register` deve garantir unicidade atômica pelo Firebase UID e criar contas sem permissões: nunca atribuir ADMIN nem escolher mentor/mentorado. Duplicidade gera `ACCOUNT_ALREADY_EXISTS`. `findByFirebaseUid` retorna vazio somente para conta inexistente. O serviço dono dos dados e o banco ainda serão definidos. Não unir identidades automaticamente pelo e-mail.
- Eliezer: implementar `UberHubSessionService`. `issue` recebe a conta consultada. `refresh` valida credenciais, status atual, revogação e reutilização e aplica eventual rotação atomicamente. `TokenPair.expiresIn` representa segundos de validade do access token; nenhuma duração está fixada nesta base.
- Arthur/Fernandinho: implementar `PasswordRecoveryGateway`, incluindo envio das instruções. Gerar link não equivale a enviar e-mail. Contas inexistentes ou inelegíveis retornam normalmente; falhas operacionais geram `DEPENDENCY_UNAVAILABLE`. Não converter contas Google em contas com senha.

Essas interfaces são contratos para entregas futuras, não implementações dos endpoints. O backend não terá campos de senha nesses contratos.

## Modelos e erros

`VerifiedIdentity` transporta identidade confiável; seu e-mail pode estar ausente. `UserAccount` representa dados necessários à autenticação, com cópia imutável das permissões, sem ser entidade de banco. `TokenPair` transporta tokens e evita expô-los em `toString`.

Os adaptadores devem usar `AuthException` e `AuthErrorCode` para falhas conhecidas. O tratamento HTTP será entregue junto com o primeiro endpoint; ainda não existe nesta base. Não registrar credenciais ou tokens. A exigência de e-mail verificado e o vínculo entre provedores continuam pendentes de definição da equipe.

## Testes e integração futura

Na pasta do auth-service, executar `sh mvnw verify`. O wrapper original não possui permissão de execução direta neste checkout.

O teste `contextLoads` usa `@MockitoBean` para as quatro interfaces apenas em `src/test`. Não há mocks ou adaptadores fictícios em produção. Esse teste verifica o carregamento do contexto, não as integrações Firebase, banco ou sessão.

O adaptador Firebase implementa o contrato de verificação; as demais interfaces continuam sem implementações nesta etapa. Nas próximas entregas, os serviços exigirão implementações reais. A configuração padrão de segurança permanece intacta; as regras de acesso aos futuros endpoints deverão ser integradas pelo responsável por segurança.

Não há migrações, entidades ou dependências novas nesta entrega. As versões existentes permanecem preservadas.

## Integração Firebase incorporada do Arthur

Autoria original: **Arthur Santana Cavichioli (Arthur Cavich)**.
Origem: https://github.com/ArthurCavich/uberhub-mentorias-backend/pull/2
Snapshot da main de origem: `4ac5a29a03089f5c9b9e0edff08b1f56b53af532`.

`FirebaseConfig.java`, `FirebaseIdentityVerifierAdapter.java` e `FirebaseIdentityVerifierAdapterTest.java` foram copiados integralmente, preservando os comentários de autoria. Os contratos e modelos oficiais não foram substituídos, e nenhuma regra de negócio das quatro APIs foi alterada. O histórico do monorepositório não foi importado.

### Configuração local

`firebase.enabled=false` é o padrão, preservando a escolha do Arthur. Para habilitar explicitamente, configure `GOOGLE_APPLICATION_CREDENTIALS` com o caminho absoluto de uma credencial válida fora do repositório e execute:

```sh
sh mvnw spring-boot:run -Dspring-boot.run.arguments="--firebase.enabled=true"
```

O SDK obtém credenciais por `GoogleCredentials.getApplicationDefault()`. Não foi introduzido caminho fixo nem conteúdo de credencial. Nenhuma consulta real ao Firebase foi executada nesta incorporação. Não reutilizar credenciais anteriormente expostas.

### Comportamento preservado e pontos para análise posterior

- `verifyIdToken(idToken, true)` verifica o token com checagem de revogação e retorna UID, e-mail e estado de verificação.
- Token nulo/vazio e `IllegalArgumentException` resultam em `INVALID_FIREBASE_TOKEN`; `IllegalStateException` resulta em `DEPENDENCY_UNAVAILABLE`.
- Todas as `FirebaseAuthException` são convertidas em `INVALID_FIREBASE_TOKEN`, inclusive possíveis falhas operacionais do SDK. Essa classificação foi preservada para revisão posterior.
- A configuração usa a instância padrão existente quando qualquer FirebaseApp já está registrada. O caso de existir somente uma instância nomeada merece validação posterior.
- O adaptador usa `@ConditionalOnBean(FirebaseAuth.class)` em um componente descoberto por varredura. A presença do adaptador com Firebase habilitado deve ser verificada em teste de contexto específico, pois condições dependem da ordem de registro.
- O teste de contexto existente usa mocks e Firebase desabilitado. Os testes originais do Arthur instanciam o adaptador com Mockito; não comprovam credenciais, conexão real ou inicialização com Firebase habilitado.
- Não foi adicionado teste com credenciais reais nem alterada a implementação para resolver esses pontos nesta etapa.
