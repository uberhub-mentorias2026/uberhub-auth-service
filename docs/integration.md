# Base dos contratos de autenticação

Esta entrega contém os quatro endpoints de autenticação, DTOs, coordenação dos fluxos e tratamento HTTP de erros, além dos contratos compartilhados e da integração Firebase Admin desenvolvida por Arthur Cavich. Não implementa banco, JWT, envio de e-mail ou filtros de segurança. O Profile Service permanece intacto.

## Responsabilidades e contratos

- Arthur: implementação original de `FirebaseIdentityVerifier` incorporada por meio de `FirebaseIdentityVerifierAdapter`, validando assinatura, expiração, projeto Firebase e a política acordada de revogação antes de retornar `VerifiedIdentity`. Google e e-mail/senha utilizam o mesmo contrato. Não basta decodificar o token.
- Illana/Eliezer: implementar `UserAccountPort`. A operação `register` deve garantir unicidade atômica pelo Firebase UID e criar contas sem permissões: nunca atribuir ADMIN nem escolher mentor/mentorado. Duplicidade gera `ACCOUNT_ALREADY_EXISTS`. `findByFirebaseUid` retorna vazio somente para conta inexistente. O serviço dono dos dados e o banco ainda serão definidos. Não unir identidades automaticamente pelo e-mail.
- Eliezer: implementar `UberHubSessionService`. `issue` recebe a conta consultada. `refresh` valida credenciais, status atual, revogação e reutilização e aplica eventual rotação atomicamente. `TokenPair.expiresIn` representa segundos de validade do access token; nenhuma duração está fixada nesta base.
- Arthur/Fernandinho: implementar `PasswordRecoveryGateway`, incluindo envio das instruções. Gerar link não equivale a enviar e-mail. Contas inexistentes ou inelegíveis retornam normalmente; falhas operacionais geram `DEPENDENCY_UNAVAILABLE`. Não converter contas Google em contas com senha.

As interfaces isolam as integrações usadas pelos endpoints. O backend não recebe senhas nos contratos das APIs.

## Modelos e erros

`VerifiedIdentity` transporta identidade confiável; seu e-mail pode estar ausente. `UserAccount` representa dados necessários à autenticação, com cópia imutável das permissões, sem ser entidade de banco. `TokenPair` transporta tokens e evita expô-los em `toString`.

Os adaptadores devem usar `AuthException` e `AuthErrorCode` para falhas conhecidas. AuthExceptionHandler converte as falhas em respostas ProblemDetail sem expor detalhes internos. Não registrar credenciais ou tokens. A exigência de e-mail verificado e o vínculo entre provedores continuam pendentes de definição da equipe.

## Testes e integração futura

Na pasta do auth-service, executar `sh mvnw verify`. O wrapper original não possui permissão de execução direta neste checkout.

O teste `contextLoads` usa `@MockitoBean` para as quatro interfaces apenas em `src/test`. Não há mocks ou adaptadores fictícios em produção. Esse teste verifica o carregamento do contexto, não as integrações Firebase, banco ou sessão.

O adaptador Firebase implementa o contrato de verificação; as demais interfaces continuam sem implementações nesta etapa. AuthService exige beans reais de UserAccountPort, UberHubSessionService e PasswordRecoveryGateway para execução fora dos testes. A configuração padrão de segurança permanece intacta; as regras de acesso aos futuros endpoints deverão ser integradas pelo responsável por segurança.

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

## Endpoints de autenticação

- `POST /auth/register`: recebe `idToken`, verifica a identidade e solicita cadastro único pelo Firebase UID; retorna 201 com `userId`. Cadastro repetido retorna 409. Não atribui permissões nem emite tokens.
- `POST /auth/login`: recebe `idToken`, consulta a conta pelo UID, verifica status e solicita tokens UberHub; retorna 200. Conta ausente retorna 409 `REGISTRATION_REQUIRED`, sem cadastro automático; conta inativa retorna 403.
- `POST /auth/refresh`: recebe `refreshToken` UberHub, delega validação/renovação atômica ao componente de sessão e retorna os tokens que o cliente deve conservar.
- `POST /auth/recover-password`: recebe `email` e solicita recuperação; retorna mensagem genérica, também para contas inexistentes ou inelegíveis conforme o contrato do gateway. Gerar link não equivale a enviar e-mail.

Google e e-mail/senha usam o mesmo ID Token Firebase. Primeiro acesso: autenticar no Firebase → login → `REGISTRATION_REQUIRED` → register → login. O frontend também pode chamar register diretamente após autenticação em um fluxo explícito de cadastro. Senhas nunca pertencem aos DTOs. Campos extras de identidade/permissões não são encaminhados à persistência. Mentor/mentorado continuam pendentes; ADMIN não é atribuído pelo cadastro.

Respostas com tokens usam `Cache-Control: no-store`. Erros usam `ProblemDetail` com códigos estáveis; logs/toString não devem expor tokens. O contrato OpenAPI está em `auth-api.yaml` (JSON válido como YAML 1.2).

Os testes HTTP usam MockMvc isolado e os testes de serviço usam Mockito. Não comprovam a SecurityFilterChain real. Eliezer deve integrar regras de acesso, CSRF/CORS e limites de requisições; login/register não exigem access token UberHub anterior. Bloqueio/revogação Firebase não revoga automaticamente sessões próprias do UberHub.

Os testes originais do Arthur e seu código foram preservados. As limitações listadas na seção Firebase continuam aplicáveis. Nenhuma conexão com credenciais reais foi executada nesta entrega.
