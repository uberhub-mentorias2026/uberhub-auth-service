# Testes manuais das APIs de autenticação com Postman

Este guia descreve como executar as quatro APIs (`POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/recover-password`) localmente, sem Firebase, MongoDB ou JWT reais.

> **Uso exclusivo local.** O perfil `postman-mock` substitui as interfaces externas por implementações fictícias e determinísticas. Não usar em produção. Não há credenciais, tokens ou dados reais.

## 1. Pré-requisitos

- JDK 21 no `PATH`.
- Porta `8081` livre em `127.0.0.1`.
- Postman (ou Newman, para execução por linha de comando).

## 2. Iniciar o servidor

Na raiz do projeto:

```sh
sh scripts/start-auth-mock.sh
```

O script:
- verifica se o Java 21 está disponível;
- verifica se a porta `8081` está livre;
- inicia o Spring Boot com o perfil Maven `local-mock` e o perfil `postman-mock`, somente em `127.0.0.1:8081`.

Equivalente manual: `sh mvnw -B -Plocal-mock spring-boot:run -Dspring-boot.run.profiles=postman-mock`.

Quando aparecer `Tomcat started on port 8081`, o servidor está pronto. Para testar a disponibilidade:

```sh
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://127.0.0.1:8081/auth/login \
  -H 'Content-Type: application/json' -d '{"idToken":"mock-invalid"}'
```

A resposta esperada é `401`.

**Encerrar:** pressione `Ctrl+C` no terminal onde o script está rodando.

## 3. Importar no Postman

1. Abra o Postman e clique em **Import**.
2. Selecione `postman/UberHub-Auth.postman_collection.json`.
3. Importe também `postman/UberHub-Local.postman_environment.json`.
4. No canto superior direito, selecione o environment **UberHub Local**.

O environment define:

| Variável | Valor | Uso |
| --- | --- | --- |
| `baseUrl` | `http://127.0.0.1:8081` | Base de todas as requisições. |
| `idToken` | `mock-existing-user` | Token fictício usado pelo request 05 (login com sucesso). |
| `refreshToken` | `mock-refresh-valid` | Token fictício usado pelo request 09 (renovação). |

## 4. Como os cenários são selecionados

Os cenários são escolhidos pelo **valor fictício** enviado no corpo da requisição. Não há endpoint de teste nem cabeçalho especial: a seleção existe somente quando o perfil `postman-mock` está ativo e o servidor está em `127.0.0.1`.

| Entrada | Resultado |
| --- | --- |
| `idToken` = `mock-new-user` | Identidade sem conta local. Cadastro: 201. Login: 409 `REGISTRATION_REQUIRED`. |
| `idToken` = `mock-existing-user` | Conta ativa. Cadastro: 409 `ACCOUNT_ALREADY_EXISTS`. Login: 200. |
| `idToken` = `mock-inactive-user` | Conta inativa. Login: 403 `ACCOUNT_INACTIVE`. |
| Qualquer outro `idToken` | 401 `INVALID_FIREBASE_TOKEN`. |
| `refreshToken` = `mock-refresh-valid` | Renovação: 200 com refresh rotacionado. |
| `refreshToken` = `mock-refresh-inactive` | 403 `ACCOUNT_INACTIVE`. |
| Qualquer outro `refreshToken` | 401 `INVALID_REFRESH_TOKEN`. |
| `email` = `unavailable@example.test` | 503 `DEPENDENCY_UNAVAILABLE`. |
| Outro e-mail válido | 200 com a mensagem genérica. |
| Corpo sem o campo obrigatório | 400 `VALIDATION_ERROR`. |

Os cenários são **sem estado**: uma identidade `mock-new-user` continua sem conta mesmo depois de um cadastro bem-sucedido. Isso torna cada requisição reproduzível.

## 5. Requisições da Collection

| # | Pasta | Requisição | Resultado esperado |
| --- | --- | --- | --- |
| 01 | Cadastro | Cadastro - sucesso | 201, somente `userId` |
| 02 | Cadastro | Cadastro - ID Token ausente | 400 `VALIDATION_ERROR` |
| 03 | Cadastro | Cadastro - ID Token inválido | 401 `INVALID_FIREBASE_TOKEN` |
| 04 | Cadastro | Cadastro - duplicado | 409 `ACCOUNT_ALREADY_EXISTS` |
| 05 | Login | Login - sucesso | 200, tokens fictícios, `Cache-Control: no-store` |
| 06 | Login | Login - sem cadastro | 409 `REGISTRATION_REQUIRED` |
| 07 | Login | Login - conta inativa | 403 `ACCOUNT_INACTIVE` |
| 08 | Login | Login - ID Token inválido | 401 `INVALID_FIREBASE_TOKEN` |
| 09 | Refresh | Refresh - renovação | 200, refresh rotacionado, `no-store` |
| 10 | Refresh | Refresh - token inválido | 401 `INVALID_REFRESH_TOKEN` |
| 11 | Recuperação | Recuperação - solicitação | 200, mensagem genérica |
| 12 | Recuperação | Recuperação - e-mail inválido | 400 `VALIDATION_ERROR` |
| 13 | Recuperação | Mensagem genérica anti-enumeração | 200, mesma mensagem da 11 |
| 14 | Recuperação | Recuperação - indisponibilidade | 503 `DEPENDENCY_UNAVAILABLE` |

## 6. Executar um cenário

1. Abra a requisição.
2. Clique em **Send**.
3. Veja a aba **Test Results**. Cada requisição tem seus próprios testes.

## 7. Executar a Collection inteira

No Postman: clique na Collection, depois em **Run**, selecione o environment **UberHub Local** e clique em **Run UberHub Auth**.

Por linha de comando (Newman):

```sh
npx --yes newman@6 run postman/UberHub-Auth.postman_collection.json \
  -e postman/UberHub-Local.postman_environment.json
```

Resultado validado nesta entrega: 14 requisições, 125 asserções, 0 falhas.

## 8. Interpretar as respostas

**Sucesso (2xx):** corpo JSON com o contrato de `docs/auth-api.yaml`. Em login e renovação, a resposta traz `Cache-Control: no-store`.

**Erro (4xx/5xx):** `Content-Type: application/problem+json`, com `status`, `title`, `detail`, `instance` e `code`. O campo `code` é o identificador estável para o cliente. Erros de validação trazem `fieldErrors` com o campo e a mensagem.

**Verificações automatizadas da Collection** (aplicadas a todas as requisições):
- o status HTTP está entre os documentados para a rota em `docs/auth-api.yaml`;
- erros da API trazem um `code` do contrato;
- respostas de erro não contêm stack trace nem ecoam os valores enviados;
- respostas de login e renovação têm `Cache-Control: no-store`.

## 9. Testes automatizados do projeto

O perfil também tem testes JUnit que cobrem os mesmos cenários com o contexto real. Eles ficam em `src/local-mock/test/` e só são compilados com o perfil Maven `local-mock`:

```sh
sh mvnw -B verify                 # build de produção: 67 testes, sem o ambiente local
sh mvnw -B -Plocal-mock verify    # inclui o ambiente local: 90 testes
```

Classes: `PostmanMockScenariosTest` (cenários) e `PostmanMockGuardTest` (bloqueios do perfil).

## 9.1 Isolamento do artefato de produção

As fontes do ambiente local ficam fora de `src/main`:

| Caminho | Conteúdo |
| --- | --- |
| `src/local-mock/java/` | Guarda, configuração do perfil e interfaces simuladas |
| `src/local-mock/resources/` | `application-postman-mock.properties` |
| `src/local-mock/test/java/` | Testes dos cenários e da guarda |

O `pom.xml` só inclui esses diretórios no perfil Maven `local-mock` (usa `build-helper-maven-plugin`). Verificação feita nesta entrega:

- `sh mvnw -B clean package` gera um JAR sem nenhuma classe `postmanmock` nem o arquivo `application-postman-mock.properties`.
- `sh mvnw -B -Plocal-mock clean package` gera um JAR com essas classes, usado apenas localmente.

A guarda em tempo de execução (`PostmanMockGuard`) continua ativa como segunda barreira.

## 10. Diferenças em relação à integração real

| Aspecto | Perfil `postman-mock` | Integração real |
| --- | --- | --- |
| Firebase | Verificador fictício por valor do token | Validação criptográfica do ID Token no projeto do App |
| Contas | Sem persistência; cenários fixos | MongoDB, unicidade atômica por UID |
| Tokens | Strings `fake-*` / `mock-*` | JWT assinado, com rotação e revogação |
| Recuperação | Sem envio de e-mail; falha simulada por valor | Envio real pelo Firebase |
| Segurança | Cadeia exclusiva que libera apenas as quatro rotas POST | Configuração global da equipe (pendente) |
| Rede | `127.0.0.1:8081` | Gateway e ambiente de teste |

Os testes com o perfil validam o **contrato HTTP** e a **orquestração** dos controllers e do serviço. Não validam Firebase, banco, JWT, envio de e-mail nem a configuração final de segurança.

## 11. Limitações conhecidas

- Os cenários não persistem nada entre requisições.
- Outras rotas são negadas (403) pela cadeia do perfil. Por isso o `405` do Spring não aparece neste ambiente.
- Não há limite de taxa neste perfil.
- A proteção contra uso fora do ambiente local depende de `server.address=127.0.0.1` e da ausência do perfil em produção. A guarda recusa combinações perigosas na inicialização (`PostmanMockGuard`), mas não impede que alguém ative o perfil em uma máquina que não seja a local.
- Cada requisição da Collection usa valores fictícios. Não colar tokens reais no environment.

## 12. Encerrar

Pressione `Ctrl+C` no terminal do `scripts/start-auth-mock.sh`. Confirme que a porta foi liberada:

```sh
lsof -nP -iTCP:8081 -sTCP:LISTEN || echo "porta livre"
```
