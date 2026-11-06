# Base dos contratos de autenticação

Esta entrega contém somente interfaces, modelos e erros compartilhados. Não contém controllers, DTOs HTTP, serviços de coordenação nem endpoints de cadastro, login, renovação ou recuperação. Não implementa Firebase, banco, JWT, envio de e-mail ou filtros de segurança. O Profile Service permanece intacto.

## Responsabilidades e contratos

- Arthur: implementar `FirebaseIdentityVerifier`, validando assinatura, expiração, projeto Firebase e a política acordada de revogação antes de retornar `VerifiedIdentity`. Google e e-mail/senha utilizam o mesmo contrato. Não basta decodificar o token.
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

As interfaces ainda não são dependências de serviços de produção nesta etapa. Nas próximas entregas, os serviços exigirão implementações reais. A configuração padrão de segurança permanece intacta; as regras de acesso aos futuros endpoints deverão ser integradas pelo responsável por segurança.

Não há migrações, entidades ou dependências novas nesta entrega. As versões existentes permanecem preservadas.
