# UberHub Mentorias — Auth Service

Serviço de autenticação do UberHub Mentorias: cadastro, login, renovação de sessão e recuperação de senha, com contratos para identidade Firebase, contas, sessões e permissões.

Repositório oficial: [uberhub-mentorias2026/uberhub-auth-service](https://github.com/uberhub-mentorias2026/uberhub-auth-service).

**Estado revisado em 18/11/2026:** main em `32c3555`, após os PRs #1–#5. As quatro APIs e seus testes com mocks estão implementados; há adaptador Firebase e persistência MongoDB parcial. JWT, envio de recuperação, integração real de contas e segurança final permanecem pendentes. O erro de inicialização encontrado nessa main foi corrigido localmente nesta branch; veja os comandos e resultados abaixo. A integração de produção permanece pendente.

## Funcionalidades

| API (POST) | Função implementada | Dependência pendente |
| --- | --- | --- |
| `/auth/register` | Verificar identidade e solicitar cadastro; 201 com `userId`, 409 para duplicidade | Conta real e conclusão da persistência MongoDB |
| `/auth/login` | Exigir conta cadastrada e ativa; solicitar tokens | Consulta real de contas e emissão JWT |
| `/auth/refresh` | Encaminhar renovação e retornar tokens | Sessão real, revogação e rotação |
| `/auth/recover-password` | Solicitar recuperação com resposta genérica | Gateway e envio real de e-mail |

Cadastro/login recebem ID Token Firebase, refresh recebe token UberHub e recuperação recebe e-mail. Nenhuma API recebe senha. Validação, erros padronizados e `Cache-Control: no-store` nas respostas de tokens estão implementados. APIs de gestão de usuários não estão presentes nesta main.

## Tecnologias

Java 21, Maven Wrapper, Spring Boot 4.1.2, Web MVC, Validation, Security, Spring Data MongoDB, Firebase Admin SDK 9.7.0 e Lombok. Testes com JUnit, Mockito, MockMvc e Postman/Newman; contrato OpenAPI 3.1.

## Documentação

- [Andamento para o professor Cadu](docs/ANDAMENTO-PROJETO.md): entregas, contribuição de cada integrante, resultados e próximos passos.
- [Integração técnica](docs/integration.md): arquitetura, responsabilidades, contratos e limites das integrações.
- [Contrato OpenAPI](docs/auth-api.yaml): requisições, respostas e erros.
- [Guia de testes Postman](docs/POSTMAN-TESTES.md): cenários da entrega do PR #4.
- [Collection](postman/UberHub-Auth.postman_collection.json) e [Environment local](postman/UberHub-Local.postman_environment.json).

## Executar nesta branch de correção

A branch `fix/auth-mongodb-local-mock` restabelece a demonstração local. A integração desta correção à main depende da revisão e do merge da branch. O histórico do bloqueio da main está nos documentos de andamento e integração; veja [o relatório da correção](docs/CORRECAO-MONGO-LOCAL.md).

Com **JDK 21** no `PATH`, na raiz deste checkout:

```sh
java -version
sh scripts/start-auth-mock.sh
```

O script usa `http://127.0.0.1:8081`. Aguarde `Tomcat started` e mantenha o terminal aberto. Não precisa de Firebase, MongoDB ou credenciais reais. Encerre com `Ctrl+C`.

Se a porta 8081 estiver ocupada, use a alternativa validada nesta revisão:

```sh
sh mvnw -B -Plocal-mock spring-boot:run \
  -Dspring-boot.run.profiles=postman-mock \
  -Dspring-boot.run.arguments=--server.port=18081
```

O perfil **Maven** `local-mock` inclui as fontes locais; o perfil **Spring** `postman-mock` ativa as dependências fictícias. Ambos são necessários. O perfil local não cria cliente, template ou repositório MongoDB; usa a mesma orquestração das APIs sem persistência. A guarda mantém Firebase desabilitado, endereço `127.0.0.1` e recusa `prod`/`production`.

## Testar com Postman

Importe a Collection e o Environment da pasta `postman/`. Selecione **UberHub Local** e confira `baseUrl`: `http://127.0.0.1:8081`, ou `http://127.0.0.1:18081` para o comando alternativo.

| POST | JSON para sucesso |
| --- | --- |
| `/auth/register` | `{"idToken":"mock-new-user"}` |
| `/auth/login` | `{"idToken":"mock-existing-user"}` |
| `/auth/refresh` | `{"refreshToken":"mock-refresh-valid"}` |
| `/auth/recover-password` | `{"email":"person@example.test"}` |

Os cenários são sem estado: cadastrar `mock-new-user` não cria uma conta para login. Use `mock-existing-user` para o login bem-sucedido. Tokens são fictícios e nenhum e-mail é enviado. Para executar os 14 cenários, use **Run** na Collection, ou Newman:

```sh
npx --yes newman@6 run postman/UberHub-Auth.postman_collection.json \
  -e postman/UberHub-Local.postman_environment.json \
  --env-var baseUrl=http://127.0.0.1:18081
```

Ajuste a porta conforme o servidor iniciado. Contratos e cenários foram preservados.

## Testes automatizados

```sh
sh mvnw -B clean verify
sh mvnw -B -Plocal-mock clean verify
```

Resultados desta correção: **69 testes padrão e 93 locais**, sem falhas ou erros. A Collection original passou via Newman com **14 requisições e 125 asserções**, sem falhas, contra o servidor em 18081. Novos testes verificam injeção/gravação com repositório simulado e ausência de infraestrutura MongoDB no perfil local.

## Integrações reais

A gravação MongoDB adicionada por Illana foi preservada no serviço normal. A configuração base agora recebe a URI por configuração externa (`SPRING_MONGODB_URI`), sem credencial literal versionada nesta branch. A credencial exposta anteriormente ainda precisa ser rotacionada no Atlas.

A execução completa sem mocks ainda depende das implementações reais de `UserAccountPort`, `UberHubSessionService` e `PasswordRecoveryGateway`, da integração MongoDB e da segurança final. O Firebase usa Application Default Credentials e vem desabilitado por padrão. Habilitar apenas Firebase não conclui as demais integrações.

Os testes atuais não comprovam Atlas, Firebase, JWT ou envio de e-mail reais. Consulte [integration.md](docs/integration.md) para as pendências e autoria das entregas.
