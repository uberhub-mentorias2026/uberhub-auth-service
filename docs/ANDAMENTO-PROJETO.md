# Andamento do projeto — UberHub Mentorias Auth Service

> **Atualização local posterior à revisão da main:** a branch `fix/auth-mongodb-local-mock` corrige a inicialização e isola MongoDB no Postman. Validação: 69 testes padrão, 93 locais e 125 asserções Newman aprovados. A integração da correção à main depende de revisão e merge; o diagnóstico abaixo registra a main `32c3555`. Consulte [a correção e os comandos atuais](CORRECAO-MONGO-LOCAL.md).

Apresentação de andamento ao professor Cadu. Revisão: **18/11/2026**.

Base: [repositório oficial](https://github.com/uberhub-mentorias2026/uberhub-auth-service), branch `main`, commit [`32c3555`](https://github.com/uberhub-mentorias2026/uberhub-auth-service/commit/32c3555925af40c27fa8467acbc8eb226216572c). Os PRs #1 a #5 estão integrados. O documento considera o código dessa versão, sem incluir alterações locais ou entregas de outros repositórios.

**Situação geral:** as quatro APIs de autenticação estão implementadas na camada de entrada e coordenação dos fluxos. Há testes com dependências simuladas, código de integração Firebase e persistência MongoDB parcial. A autenticação completa em ambiente real ainda não está comprovada, e a versão atual apresenta um bloqueio de inicialização que precisa ser corrigido antes da demonstração.

## 1. Objetivo do Auth Service

Centralizar o acesso ao UberHub Mentorias: cadastrar a conta local a partir de uma identidade Firebase, iniciar e renovar sessões e solicitar recuperação de senha. O serviço também estabelece contratos para contas e permissões. Dados de perfil pertencem a um serviço separado, fora do escopo desta revisão.

## 2. Tecnologias utilizadas

- Java 21, Maven e Maven Wrapper.
- Spring Boot 4.1.2, Spring Web MVC e Bean Validation para as APIs.
- Firebase Admin SDK 9.7.0 para verificação de identidade.
- Spring Data MongoDB e Lombok para o modelo de persistência; há configuração para MongoDB Atlas.
- Spring Security, com configuração específica para demonstração local; segurança final pendente.
- JUnit, Mockito e MockMvc para testes automatizados; Postman/Newman para cenários HTTP.
- OpenAPI 3.1 para o contrato das APIs; Git/GitHub para histórico e revisão de entregas.

As versões são as declaradas no projeto. JWT próprio do UberHub é uma integração prevista, ainda sem implementação real nesta main.

## 3. Divisão de responsabilidades da equipe

| Integrante | Responsabilidade | Situação verificada |
| --- | --- | --- |
| Cristian Passos da Silva | Base, quatro APIs, contratos, documentação e testes | PRs #1, #3 e #4 integrados; também incorporou a contribuição Firebase de Arthur no PR #2. |
| Arthur Santana Cavichioli | Integração Firebase | Código original de configuração e verificação incorporado no PR #2. |
| Illana Rabelo | Persistência MongoDB | Entidade, repositório e gravação condicional integrados no PR #5. |
| Illana / Eliezer | Integração das contas usadas pela autenticação | Responsabilidade prevista nos contratos; implementação real pendente. |
| Eliezer | Sessões/JWT e segurança | Responsabilidade prevista; implementação final ainda pendente. |
| Arthur / Fernandinho | Recuperação de senha e envio de instruções | Responsabilidade prevista; envio real ainda pendente. |

A tabela distingue autoria de entregas integradas e responsabilidades registradas no planejamento técnico. Não atribui como concluído trabalho ausente da main.

## 4. Funcionalidades implementadas

| Funcionalidade | Código implementado | O que a evidência permite afirmar |
| --- | --- | --- |
| Cadastro | Recebe identidade Firebase e coordena criação de conta; trata duplicidade. | Fluxo testado com mocks; persistência completa ainda pendente. |
| Login | Exige cadastro existente e conta ativa; solicita tokens. | Regras testadas com mocks; tokens reais não implementados. |
| Renovação de sessão | Recebe refresh token e encaminha renovação. | Contrato e delegação testados; validação/rotação reais pendentes. |
| Recuperação de senha | Recebe e-mail e responde com mensagem genérica. | Comportamento testado; entrega de e-mail não implementada. |
| Validação e erros | Trata campos inválidos, JSON inválido e falhas conhecidas sem expor detalhes internos. | Testes HTTP isolados aprovados nesta revisão. |
| Demonstração local | Perfil com identidades, contas e tokens fictícios; Collection de 14 requisições. | Funcionamento registrado no PR #4; execução da main atual bloqueada. |

**Código implementado** significa que o comportamento está escrito no repositório. **Teste com mocks** confirma respostas sob dependências simuladas. **Integração real comprovada** exige evidência de execução com os serviços externos; não foi encontrada comprovação de ponta a ponta nas fontes consultadas. **Pendente** identifica implementação ausente ou validação ainda necessária.

## 5. Minha contribuição: cadastro, login, refresh e recuperação de senha

Contribuição de **Cristian Passos da Silva**, integrada no PR #3:

- **Cadastro — `POST /auth/register`:** recebe ID Token Firebase, solicita cadastro pelo identificador do usuário e retorna o ID da conta. Não recebe senha, não concede ADMIN nem emite sessão no cadastro.
- **Login — `POST /auth/login`:** verifica identidade, exige conta já cadastrada e ativa e solicita a sessão UberHub. No primeiro acesso, orienta o cliente a cadastrar antes de repetir o login.
- **Refresh — `POST /auth/refresh`:** recebe o refresh token e encaminha a renovação ao componente de sessão, devolvendo os tokens retornados por ele.
- **Recuperação — `POST /auth/recover-password`:** recebe e-mail e solicita recuperação, com a mesma mensagem para contas elegíveis, desconhecidas ou inelegíveis, conforme o contrato da integração.

A entrega inclui validações, respostas de erro padronizadas, proteção contra cache das respostas de tokens, contrato OpenAPI e testes. Os quatro commits de implementação são `3fafb55` (cadastro), `6ba99c1` (login), `f461225` (refresh) e `5618610` (recuperação), seguidos dos ajustes de `0d4b376`.

Também preparei a base de contratos no PR #1 e o ambiente Postman no PR #4. O código Firebase original é de Arthur; a persistência acrescentada no PR #5 é de Illana. A implementação das APIs coordena essas integrações, sem substituir as entregas dos colegas.

## 6. Integração Firebase

O adaptador incorporado no PR #2 usa o Firebase Admin SDK para verificar ID Tokens com checagem de revogação e obter identidade confiável. O contrato aceita identidade originada de Google ou de e-mail/senha; a senha não passa pelas APIs do backend.

**Implementado:** configuração e código de verificação. **Testado com mocks:** comportamento do adaptador perante respostas simuladas do SDK. **Ainda não comprovado:** inicialização completa com Firebase habilitado e autenticação com tokens reais, incluindo expiração e revogação. O Firebase vem desabilitado na configuração padrão.

A classificação de falhas operacionais e alguns casos de inicialização ainda precisam de validação. Recuperação com envio real de e-mail não está concluída.

## 7. Persistência MongoDB

O PR #5, integrado em 17/11/2026, criou o modelo de credenciais, sua consulta por identidade Firebase e uma gravação condicional no cadastro. Portanto, o projeto já possui código de persistência MongoDB.

**Estado: parcial.** Esse armazenamento ainda não fornece a conta completa consultada no login. Falta integrar identificador da conta, status e permissões, garantir unicidade sob cadastros simultâneos e testar leitura/gravação reais. Não há resultado de teste Atlas apresentado no PR #5; suas instruções de teste não comprovam execução bem-sucedida.

A configuração contém uma credencial de banco versionada. A equipe precisa rotacioná-la e passar a fornecer a configuração externamente. Nenhum segredo é reproduzido aqui.

## 8. JWT, segurança e APIs de usuários

O formato de resposta de tokens e o contrato de sessão estão definidos. Ainda faltam emissão e validação reais de JWT, expiração, revogação, controle de reutilização e renovação segura. Os tokens da demonstração são fictícios.

Já existem validações de entrada, mensagens de erro sem detalhes internos, ocultação de tokens em representações de objetos e proteção contra cache nas respostas de login/refresh. A segurança configurada para o ambiente local não representa a configuração de produção.

Faltam regras finais de acesso, CORS/CSRF, limite de requisições e autorização por permissões. A main contém apenas o controller de autenticação; APIs de gestão de usuários não estão implementadas neste repositório. Seu escopo e integração com o serviço de perfis devem ser alinhados pela equipe. Revogar identidade Firebase não revoga automaticamente uma sessão própria do UberHub.

## 9. Testes realizados e resultados

### Resultados históricos registrados nos PRs

| Entrega | Resultado registrado | Limite da evidência |
| --- | --- | --- |
| PR #1 | 1 teste de contexto aprovado | Interfaces externas simuladas. |
| PR #3 | 25, 35, 41 e 49 testes aprovados nas etapas sucessivas | Contagens cumulativas, não somáveis; testes com mocks, sem integração externa real. |
| PR #4 — build padrão | 67 testes, sem falhas | Resultado anterior ao PR #5. |
| PR #4 — perfil local | 90 testes, sem falhas | Contexto local com dependências fictícias. |
| PR #4 — Newman | 14 requisições, 125 asserções, sem falhas | HTTP real contra servidor local com mocks; não comprova Firebase, Atlas, JWT ou e-mail. |

### Resultado da revisão da main em 18/11/2026

A suite foi executada em cópia limpa do commit de referência, com JDK 21 e URI MongoDB substituída por endereço local sem credenciais. Não foram usados os serviços Firebase/Atlas reais.

| Execução | Aprovados | Erros | Resultado |
| --- | --- | --- | --- |
| `sh mvnw -B verify` | 66 de 67 | 1 | Build falhou no teste de contexto. |
| `sh mvnw -B -Plocal-mock clean verify` | 72 de 90 | 18 | Build falhou no contexto e nos cenários locais. |

Em ambos os casos houve zero falhas de asserção e zero testes ignorados. Os erros decorrem do carregamento do contexto: o Spring não consegue construir o serviço de autenticação na configuração atual (`No default constructor found`). Assim, os cenários locais não chegaram a comprovar o funcionamento das rotas nessa versão.

Os testes isolados de controller, serviço, recuperação e adaptador Firebase passaram. Eles não exercitam a nova gravação MongoDB. Newman não foi reexecutado nesta revisão, a importação visual no Postman não está comprovada e não foi medida cobertura percentual.

## 10. O que ainda falta implementar ou validar

- Corrigir o bloqueio de inicialização e revalidar a aplicação após a integração MongoDB.
- Separar corretamente os perfis de compilação e execução e garantir que testes locais não gravem no banco real.
- Completar contas persistentes, unicidade e tratamento consistente de falhas de cadastro.
- Rotacionar a credencial versionada e externalizar a configuração do banco.
- Validar Firebase real e implementar sessões JWT, recuperação com entrega de e-mail e segurança final.
- Definir APIs de usuários, papéis, política de e-mail verificado e vínculo entre provedores.
- Integrar frontend/gateway/serviço de perfis e comprovar os fluxos de ponta a ponta, incluindo cenários de erro.

## 11. Próximos passos

1. **Restabelecer a execução:** corrigir a inicialização, proteger a configuração e isolar a demonstração local.
2. **Concluir as integrações:** contas MongoDB, Firebase, sessões e entrega de recuperação, com responsáveis conforme a divisão da equipe.
3. **Validar funcionalmente:** repetir suites e Collection e registrar cadastro → login → refresh, recuperação e persistência em ambiente de teste real.
4. **Preparar a apresentação:** demonstrar apenas fluxos revalidados, identificando claramente o uso de mocks e anexando resultados vinculados ao commit testado.

## 12. Referências aos PRs e documentos oficiais

| PR integrado | Entrega | Commit de integração |
| --- | --- | --- |
| [#1](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/1) | Estrutura base | `01e3671` |
| [#2](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/2) | Incorporação Firebase | `69ed537` |
| [#3](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/3) | Quatro APIs | `79daa05` |
| [#4](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/4) | Ambiente local/Postman | `02a751d` |
| [#5](https://github.com/uberhub-mentorias2026/uberhub-auth-service/pull/5) | Persistência MongoDB parcial | `32c3555` |

- [README](../README.md): visão geral e execução com as limitações atuais.
- [Integração técnica](integration.md): arquitetura, contratos, evidências e pendências detalhadas; inclui a proveniência histórica do código de Arthur.
- [OpenAPI](auth-api.yaml): contrato preservado das quatro APIs.
- [Guia Postman](POSTMAN-TESTES.md): cenários e resultados históricos do PR #4, sujeitos à revalidação após o PR #5.
- [Collection](../postman/UberHub-Auth.postman_collection.json) e [Environment](../postman/UberHub-Local.postman_environment.json): artefatos preservados de testes locais.
