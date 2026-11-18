# UberHub Mentorias — Auth Service

Serviço de autenticação do UberHub Mentorias: cadastro, login, renovação de sessão e recuperação de senha, com contratos para identidade Firebase, contas, sessões e permissões.

Repositório oficial: [uberhub-mentorias2026/uberhub-auth-service](https://github.com/uberhub-mentorias2026/uberhub-auth-service).

**Estado revisado em 18/11/2026:** main em `32c3555`, após os PRs #1–#5. As quatro APIs e seus testes com mocks estão implementados; há adaptador Firebase e persistência MongoDB parcial. JWT, envio de recuperação, integração real de contas e segurança final permanecem pendentes. A main atual apresenta erro de inicialização de `AuthService`; os comandos abaixo permitem diagnosticar o estado e não representam uma aplicação pronta para produção.

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

## Preparação e validação

Use JDK 21 no `PATH`; o Maven Wrapper baixa as dependências necessárias. Para um checkout novo:

```sh
git clone https://github.com/uberhub-mentorias2026/uberhub-auth-service.git
cd uberhub-auth-service
git switch main
java -version
```

A main contém URI MongoDB com credencial literal em `application.properties`. A equipe precisa rotacionar essa credencial e externalizar a configuração. Para os diagnósticos locais abaixo, sobrescreva a URI no terminal, sem usar Atlas ou credenciais reais:

```sh
export SPRING_MONGODB_URI='mongodb://127.0.0.1:27017/uberhub_docs_review?connectTimeoutMS=1000&serverSelectionTimeoutMS=1000'
sh mvnw -B verify
sh mvnw -B -Plocal-mock clean verify
```

Essa URI serve para diagnóstico local, não comprova persistência e não desabilita o cliente MongoDB. Não é necessário configurar Firebase real para essas suites.

**Resultado desta revisão em cópia limpa da main:** build padrão com 67 testes, 1 erro; build local com 90 testes, 18 erros. Ambos tiveram zero falhas de asserção e zero ignorados. O contexto falha ao criar `AuthService` (`No default constructor found`): existem dois construtores sem seleção explícita para injeção. Os testes isolados passam, mas o ambiente de demonstração precisa ser restabelecido. Detalhes no [andamento](docs/ANDAMENTO-PROJETO.md#9-testes-realizados-e-resultados).

## Execução local com dados fictícios

Após corrigir o bloqueio e revalidar o isolamento do banco, o comando previsto, no terminal com a URI local acima, é:

```sh
sh scripts/start-auth-mock.sh
```

Equivalente:

```sh
sh mvnw -B -Plocal-mock spring-boot:run -Dspring-boot.run.profiles=postman-mock
```

O script exige Java 21 e porta 8081 livre. O endereço configurado para esse perfil é `http://127.0.0.1:8081`; encerre com `Ctrl+C`. **8081 é a porta do perfil local; a configuração base não fixa essa porta.**

O perfil **Maven** `local-mock` inclui fontes/recursos de `src/local-mock/`; o perfil **Spring** `postman-mock` ativa os beans fictícios e a segurança local. `-Plocal-mock` sozinho não ativa esses beans. O `spring.profiles.active=local-mock` hoje presente na configuração base também não os ativa; o comando acima seleciona explicitamente o perfil correto.

A guarda restringe o ambiente a `127.0.0.1`, Firebase desabilitado e ausência de `prod`/`production`. As quatro interfaces externas são simuladas, com tokens fictícios e sem envio de e-mail. A gravação MongoDB acrescentada no PR #5 exige revisar o isolamento: não afirmar que a main atual não acessa banco apenas por usar esse perfil.

Quando a aplicação estiver novamente iniciando e isolada, importe a Collection e o Environment no Postman ou execute:

```sh
npx --yes newman@6 run postman/UberHub-Auth.postman_collection.json \
  -e postman/UberHub-Local.postman_environment.json
```

O PR #4 registrou 14 requisições e 125 asserções sem falhas, antes do PR #5. Esse resultado é histórico; Newman não foi reexecutado nesta revisão. Contratos e cenários foram preservados.

## Integrações reais

Ainda não existe procedimento completo comprovado para executar todos os fluxos reais desta main. São necessários: correção da inicialização; implementação real de `UserAccountPort`, `UberHubSessionService` e `PasswordRecoveryGateway`; configuração segura MongoDB; validação do Firebase habilitado; e segurança final.

O Firebase usa Application Default Credentials e vem desabilitado por padrão. Para futura validação, `GOOGLE_APPLICATION_CREDENTIALS` deve apontar para credencial válida fora do repositório e `firebase.enabled=true` deve ser habilitado, sem o perfil de mocks. Habilitar apenas Firebase não conclui as demais integrações. Consulte os detalhes e a autoria das entregas em [integration.md](docs/integration.md).
