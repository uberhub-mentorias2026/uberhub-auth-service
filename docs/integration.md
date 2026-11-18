# Integração do Auth Service

> **Atualização local posterior à revisão da main:** a branch `fix/auth-mongodb-local-mock` corrige a inicialização e isola MongoDB no Postman. Validação: 69 testes padrão, 93 locais e 125 asserções Newman aprovados. A integração da correção à main depende de revisão e merge; o diagnóstico abaixo registra a main `32c3555`. Consulte [a correção e os comandos atuais](CORRECAO-MONGO-LOCAL.md).

Repositório oficial: [uberhub-mentorias2026/uberhub-auth-service](https://github.com/uberhub-mentorias2026/uberhub-auth-service).

Revisão de 18/11/2026, baseada na `main` em `32c3555925af40c27fa8467acbc8eb226216572c`, após os PRs #1 a #5. O histórico Git e o estado integrado dos PRs foram conferidos. Alterações locais não integradas não compõem este diagnóstico.

## Estado da implementação

As quatro APIs possuem controller, DTOs, validação, orquestração e tratamento de erros. Isso não significa autenticação completa em ambiente real: emissão de JWT, contas persistentes acessadas pelo contrato de usuários e envio de recuperação ainda dependem de implementações da equipe.

O PR #5 adicionou a entidade MongoDB, o repositório e uma gravação condicional no cadastro. Portanto, a afirmação anterior de que não havia entidade ou persistência deixou de representar o código. A persistência ainda é parcial e não foi comprovada de ponta a ponta. A revisão também reproduziu um bloqueio de inicialização do contexto Spring; consulte “Testes e execução”.

## Arquitetura e responsabilidades

O cliente autentica no Firebase e envia o ID Token ao Auth Service. `AuthController` valida os DTOs e encaminha a `AuthService`, que coordena quatro interfaces. O serviço usa ainda `CredencialRepository` diretamente no cadastro desde o PR #5. O Profile Service é separado e está fora desta revisão.

| Integrante | Responsabilidade registrada | Entrega e limite verificados |
| --- | --- | --- |
| Cristian Passos da Silva | Base de contratos e quatro APIs de autenticação; documentação e testes | PRs #1 e #3; ambiente local e Collection no PR #4. Incorporou o código Firebase de Arthur no PR #2, preservando a autoria original. |
| Arthur Santana Cavichioli (Arthur Cavich) | Integração Firebase e verificação de identidade | Configuração e adaptador incorporados no PR #2; integração externa real ainda não comprovada nas evidências consultadas. |
| Illana Rabelo | Persistência de autenticação | PR #5: `Credencial`, `CredencialRepository` e gravação condicional no cadastro. |
| Illana / Eliezer | Contrato de contas `UserAccountPort` | Responsabilidade descrita na documentação original; implementação real ainda ausente nesta main. |
| Eliezer | Sessões/JWT e segurança | Contrato `UberHubSessionService` presente; implementação de produção e regras finais pendentes. |
| Arthur / Fernandinho | Recuperação e entrega das instruções | Contrato `PasswordRecoveryGateway` presente; implementação real pendente. |

As atribuições sem PR de implementação são responsabilidades previstas, não entregas concluídas. APIs de gestão de usuários e integração com perfis precisam de confirmação de escopo e responsáveis; não há controller de usuários nesta main.

### Contratos que devem ser preservados

- `FirebaseIdentityVerifier`: verificar assinatura, expiração, projeto e revogação antes de produzir `VerifiedIdentity`. Google e e-mail/senha usam o mesmo contrato; decodificar o token não basta.
- `UserAccountPort`: cadastro único e atômico por Firebase UID, sem permissões, ADMIN ou escolha de mentor/mentorado. Duplicidade deve gerar `ACCOUNT_ALREADY_EXISTS`. Consulta vazia significa apenas conta inexistente. Não unir identidades por e-mail. A divisão definitiva dos dados entre serviços continua pendente, embora as credenciais já tenham modelo MongoDB neste serviço.
- `UberHubSessionService`: `issue` recebe a conta consultada; `refresh` deve validar credencial, status atual, revogação e reutilização, com eventual rotação atômica. `expiresIn` expressa segundos de validade do access token; o exemplo 900 não fixa a política de produção.
- `PasswordRecoveryGateway`: solicitar e entregar instruções, sem retornar links à API nem converter contas Google em contas com senha. Conta inexistente ou inelegível deve retornar normalmente; falha operacional deve gerar `DEPENDENCY_UNAVAILABLE`. Gerar link não equivale a enviar e-mail.

## Contribuição de Cristian nas quatro APIs

A implementação está no PR #3, sobre a base do PR #1 e o adaptador do PR #2. O PR #4 acrescenta o ambiente de demonstração local.

| API | Comportamento implementado | Integração ainda necessária |
| --- | --- | --- |
| `POST /auth/register` | Recebe `idToken`, verifica identidade, solicita cadastro por UID e retorna 201 com `userId`. Duplicidade: 409. Não emite tokens nem atribui permissões. | Implementar contas reais e unicidade atômica; completar a persistência adicionada posteriormente por Illana. |
| `POST /auth/login` | Recebe `idToken`, consulta pelo UID e exige conta ativa. Conta ausente: 409 `REGISTRATION_REQUIRED`; inativa: 403. Solicita e retorna tokens com 200. | Consulta persistente de contas e emissão real de tokens UberHub. |
| `POST /auth/refresh` | Recebe `refreshToken` UberHub, delega renovação e retorna o par de tokens com 200. | Validação criptográfica, revogação, prevenção de reutilização e política de rotação reais. |
| `POST /auth/recover-password` | Recebe `email`, solicita recuperação e retorna 200 com mensagem genérica. | Gateway real e comprovação da entrega do e-mail. |

Primeiro acesso: autenticar no Firebase → login → `REGISTRATION_REQUIRED` → register → login. Também é possível chamar register diretamente após autenticação em um fluxo explícito de cadastro. A persistência desse fluxo entre chamadas ainda depende dos adaptadores reais; os cenários Postman são fictícios e sem estado.

Senhas não pertencem aos DTOs. Campos extras de identidade/permissões não são encaminhados à persistência pela API. Mentor/mentorado e política de e-mail verificado/vínculo entre provedores continuam pendentes de definição.

## Modelos, erros e segurança

`VerifiedIdentity` transporta identidade confiável, com e-mail possivelmente ausente. `UserAccount` é a visão da conta para autenticação, com cópia imutável das permissões, não uma entidade de banco. `TokenPair` transporta tokens e evita expô-los em `toString`. Os DTOs de entrada e a resposta de tokens também ocultam dados sensíveis em `toString`.

Adaptadores devem usar `AuthException` e `AuthErrorCode`. `AuthExceptionHandler` converte falhas conhecidas em `ProblemDetail`, sem detalhes internos; validação e JSON inválido resultam em 400 e falhas inesperadas em 500. Não registrar credenciais ou tokens. Login e refresh bem-sucedidos usam `Cache-Control: no-store`.

O [OpenAPI](auth-api.yaml) é JSON válido como YAML 1.2 e permanece inalterado. Preserva também as particularidades HTTP: 405 sem corpo e 415 com problema do framework, sem `code`. No perfil local, métodos/rotas não permitidos são barrados pela segurança com 403, conforme o guia Postman; isso não exige mudar o contrato nesta revisão.

Spring Security está nas dependências, mas a cadeia explícita existente é exclusiva do perfil `postman-mock`. Regras de produção, CSRF/CORS, limites de requisições e autorização por permissões ainda precisam ser integrados. Login/register não devem exigir access token UberHub anterior. Bloqueio/revogação no Firebase não revoga automaticamente sessões próprias do UberHub.

## Persistência MongoDB: implementação parcial do PR #5

- Spring Data MongoDB já estava nas dependências da base. O PR #5 adicionou `Credencial`, mapeada para `credenciais`, com `id`, `firebase_uid` e `email`, e `CredencialRepository`, com busca por Firebase UID.
- Após `users.register(identity)`, `AuthService.register` consulta e salva a credencial caso o repositório esteja presente e o UID não seja encontrado. O construtor de quatro argumentos deixa esse repositório nulo; o de cinco recebe o repositório.
- Isso não implementa `UserAccountPort`: login continua consultando essa interface; não lê `CredencialRepository`. O identificador retornado vem de `UserAccount`, sem vínculo implementado com o `id` MongoDB. O modelo de credenciais não armazena status nem permissões.
- Consultar antes de salvar não garante unicidade sob concorrência. Não há índice único declarado por UID, migração de índice ou coordenação atômica entre cadastro da conta e gravação da credencial no código consultado. Uma falha na segunda operação pode deixar estado parcial.
- Não foram adicionados testes específicos de persistência no PR #5. Os testes de serviço existentes não fornecem `CredencialRepository` e não validam a nova gravação. A descrição do PR propõe verificar Atlas, mas não apresenta resultado de execução; não foi encontrada evidência de leitura/gravação real comprovada.
- `application.properties` contém URI Atlas com credencial literal. Não reproduzir nem reutilizar esse segredo: a equipe precisa rotacioná-lo, removê-lo da configuração versionada e externalizar a URI, por exemplo via `SPRING_MONGODB_URI`. Essa correção não foi realizada na revisão documental.
- O mesmo arquivo ativa `local-mock` como perfil Spring. Esse nome é o perfil **Maven**, que inclui as fontes de teste local; o perfil **Spring** necessário para seus beans é `postman-mock`. Não são intercambiáveis.

## Integração Firebase: origem histórica e estado atual


Autoria original: **Arthur Santana Cavichioli (Arthur Cavich)**.
Origem: https://github.com/ArthurCavich/uberhub-mentorias-backend/pull/2
Snapshot da main de origem: `4ac5a29a03089f5c9b9e0edff08b1f56b53af532`.

`FirebaseConfig.java`, `FirebaseIdentityVerifierAdapter.java` e `FirebaseIdentityVerifierAdapterTest.java` foram copiados integralmente, preservando os comentários de autoria. Os contratos e modelos oficiais não foram substituídos, e nenhuma regra de negócio das quatro APIs foi alterada. O histórico do monorepositório não foi importado.

### Configuração Firebase (pré-requisito, não inicialização completa)

`firebase.enabled=false` é o padrão, preservando a escolha do Arthur. A execução completa ainda depende dos adaptadores e das correções descritas neste documento. Para habilitar explicitamente, configure `GOOGLE_APPLICATION_CREDENTIALS` com o caminho absoluto de uma credencial válida fora do repositório e execute:

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


## Testes e execução

A configuração e o adaptador Firebase descritos acima correspondem à main oficial, sem incorporar ajustes locais não commitados. A origem antiga é citada exclusivamente para registrar autoria/proveniência; o desenvolvimento atual deve usar o repositório oficial.

### Evidência histórica

O PR #3 registra validações incrementais de 25, 35, 41 e 49 testes (contagens cumulativas). O PR #4 registra 67 testes no build padrão, 90 com `local-mock` e Newman com 14 requisições/125 asserções, sem falhas. São resultados anteriores ao PR #5, não uma certificação da main atual. A importação visual no aplicativo Postman não foi testada naquela entrega.

Mockito e MockMvc isolado validam orquestração e contrato HTTP. O teste `contextLoads` usa `@MockitoBean` para as quatro interfaces e não comprova Firebase, banco ou sessão. Os testes do adaptador Firebase simulam o SDK. Os testes do perfil local exercitam contexto e cadeia de segurança locais, sem validar segurança de produção.

### Validação desta revisão (18/11/2026)

Em cópia limpa do commit `32c3555`, com JDK 21 e `SPRING_MONGODB_URI` sobrescrita para localhost sem credenciais, o comando `sh mvnw -B verify` registrou **67 testes, 0 falhas de asserção, 1 erro, 0 ignorados**: 66 passaram e `contextLoads` falhou. A causa foi a criação de `AuthService`: `No default constructor found`. Há dois construtores sem seleção explícita para injeção pelo Spring. O resultado é `BUILD FAILURE`; não é falha de autenticação no Atlas.

Com `sh mvnw -B -Plocal-mock clean verify`, na mesma cópia e com a mesma URI local, foram registrados **90 testes, 0 falhas de asserção, 18 erros, 0 ignorados**: 72 passaram; o teste de contexto e os 17 cenários locais ficaram com erro por falha de carregamento do contexto (incluindo tentativas subsequentes interrompidas pelo limite do Spring). O build também falhou.

Não foram usados Firebase/Atlas reais nem executado Newman nesta revisão. Não foi medida cobertura percentual. A inicialização precisa ser corrigida antes da demonstração da main.

### Ambiente local e limitações atuais

As fontes fictícias ficam em `src/local-mock/`, incluídas somente pelo perfil Maven `local-mock`; não fazem parte do JAR padrão. A guarda recusa `prod`/`production`, endereço diferente de `127.0.0.1` e Firebase habilitado. A cadeia local libera somente as quatro rotas POST e não define usuário padrão. Tokens são strings fictícias e recuperação não envia e-mail.

O script `sh scripts/start-auth-mock.sh` ativa os dois perfis corretamente e usa `127.0.0.1:8081`. Porém, o bloqueio de contexto após o PR #5 impede tratar esse comando como demonstração funcional da main atual. Além disso, a nova gravação no serviço não está isolada por perfil: é necessário revisar o isolamento do MongoDB antes de voltar a afirmar que esse ambiente não persiste nada.

Consulte o [README](../README.md) para comandos de diagnóstico com URI local. O [guia Postman](POSTMAN-TESTES.md) e a Collection foram preservados; seus resultados e afirmações de isolamento representam a entrega do PR #4 e devem ser revalidados após o PR #5. Nenhuma mudança de contrato ou cenário foi realizada.

## Pendências de integração

1. Resolver a criação de `AuthService`, distinguir os perfis Maven/Spring e restabelecer o ambiente local isolado.
2. Rotacionar a credencial MongoDB versionada e externalizar configuração.
3. Implementar `UserAccountPort`, alinhar IDs/status/permissões, garantir unicidade atômica e tratar falhas da persistência.
4. Completar sessões JWT e recuperação com entrega real; integrar segurança e APIs de usuários conforme o escopo acordado.
5. Validar Firebase habilitado, credenciais, tokens válidos/expirados/revogados e classificação de erros operacionais.
6. Executar novamente suites e Collection; depois registrar testes reais de cadastro → login → refresh, recuperação e persistência, incluindo concorrência e indisponibilidade.
7. Definir e-mail verificado, vínculo de provedores, papéis mentor/mentorado, URI estável dos tipos de erro e endereço do gateway (INT-03).

## Referências oficiais

- [PR #1 — base](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/1).
- [PR #2 — incorporação Firebase](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/2).
- [PR #3 — quatro APIs](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/3).
- [PR #4 — ambiente local/Postman](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/4).
- [PR #5 — persistência MongoDB](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/5).
- [Andamento para apresentação](ANDAMENTO-PROJETO.md), [OpenAPI](auth-api.yaml) e [guia Postman](POSTMAN-TESTES.md).
