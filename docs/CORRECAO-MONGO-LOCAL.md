# Correção da inicialização e isolamento MongoDB local

Validação em 18/11/2026, na branch `fix/auth-mongodb-local-mock`, baseada em `ceefcb1` (documentação sobre a main `32c3555`). Correção preparada nesta branch para revisão e integração à main. Os resultados abaixo pertencem a esta correção, não à main remota.

## Problema e correção

Após o PR #5, `AuthService` tinha dois construtores sem escolha explícita para injeção. O Spring falhava com `No default constructor found` antes de iniciar as APIs.

- O construtor com `CredencialRepository` agora está marcado com `@Autowired`; o serviço normal exige essa dependência. A consulta/gravação implementada por Illana foi preservada.
- O componente normal é selecionado fora de `postman-mock`. Dentro desse perfil, uma fábrica explícita cria a mesma classe de serviço com as quatro interfaces fictícias e sem repositório. Não há duplicação dos fluxos das APIs.
- O perfil Postman exclui as autoconfigurações de cliente, dados e repositórios MongoDB. Um teste verifica ausência de `MongoClient`, `MongoTemplate` e `CredencialRepository` no contexto.
- A configuração base não ativa mais `local-mock` como perfil Spring. O script continua ativando Maven `local-mock` e Spring `postman-mock` explicitamente.
- A URI com credencial foi retirada do arquivo de configuração desta branch. Para integração real, fornecer `SPRING_MONGODB_URI` externamente. Sem essa variável, aplica-se o comportamento padrão local do Spring Boot. A credencial anteriormente publicada continua no histórico e precisa ser rotacionada no Atlas; isso não foi executado nesta tarefa.

O construtor sem repositório continua disponível para construção explícita em testes e no ambiente local. Os testes de contexto do serviço normal simulam o repositório e excluem infraestrutura de rede; verificam que uma credencial nova é encaminhada para gravação e que uma já encontrada não é gravada de novo.

## Validação executada

| Verificação | Resultado |
| --- | --- |
| `sh mvnw -B clean verify` | 69 testes, 0 falhas, 0 erros, 0 ignorados |
| `sh mvnw -B -Plocal-mock clean verify` | 93 testes, 0 falhas, 0 erros, 0 ignorados |
| Aplicação com `postman-mock` | Tomcat iniciou em `127.0.0.1:18081` |
| Collection original via Newman 6 | 14 requisições, 125 asserções, 0 falhas |

A porta 8081 estava ocupada por um processo Node, que foi preservado. A porta alternativa foi fornecida pela linha de comando, sem mudar Collection, Environment ou contrato OpenAPI. A importação visual no aplicativo Postman não foi revalidada.

Esses resultados comprovam a demonstração HTTP com mocks e a ligação do serviço ao repositório simulado. Não comprovam conexão Atlas, unicidade atômica, Firebase real, JWT próprio ou entrega de e-mail. As pendências funcionais descritas em [integration.md](integration.md) permanecem, exceto o bloqueio e o isolamento corrigidos nesta branch.

## Executar e testar

Com JDK 21, na raiz deste checkout:

```sh
sh scripts/start-auth-mock.sh
```

Se 8081 estiver ocupada, use:

```sh
sh mvnw -B -Plocal-mock spring-boot:run \
  -Dspring-boot.run.profiles=postman-mock \
  -Dspring-boot.run.arguments=--server.port=18081
```

No Postman, importar os arquivos de `postman/`, selecionar **UberHub Local** e ajustar `baseUrl` para `http://127.0.0.1:18081` se estiver usando a porta alternativa. Os valores de teste e os 14 cenários continuam os mesmos do [guia Postman](POSTMAN-TESTES.md).

Equivalente automatizado na porta alternativa:

```sh
npx --yes newman@6 run postman/UberHub-Auth.postman_collection.json \
  -e postman/UberHub-Local.postman_environment.json \
  --env-var baseUrl=http://127.0.0.1:18081
```

Encerrar o servidor com `Ctrl+C`. O ambiente não precisa de credenciais Firebase nem MongoDB.
