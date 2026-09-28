# Documentação Microserviços
Aluno: Pedro Araujo Vieira — Matrícula: 16123288733

https://claude.ai/code/artifact/2c541174-0b49-486d-9ee2-1682e91b8daf

# Documentação implementação Docker GUIA
https://claude.ai/code/artifact/a5f3c8e5-81de-4791-a16c-a71d4aa45f0f

# Documentação implementação Kubernetes GUIA
https://claude.ai/code/artifact/68cd2944-af3d-430f-8e6f-ea27c4186982

# Documentação deploy na Oracle Cloud GUIA
https://claude.ai/artifact/71fuEpCrAzrZzCS6kyVzKo

# Documentação GitHub Actions (CI/CD) GUIA
https://claude.ai/artifact/F21uNn1PE4FpN3ewMj6Aw6

## Atividade: fornecedores-service

O serviço usa Java 17, Spring Boot, H2, Eureka, Config Server e OpenFeign.
O `auth-service` foi movido para a porta **8086**, liberando a **8084** para fornecedores.

### Execução local

Na raiz do repositório, compile cada serviço com `mvn -f NOME-DO-SERVICO/pom.xml package -DskipTests`.
Inicie os JARs em terminais separados, nesta ordem, aguardando a inicialização de cada um:

```text
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar produtos-service/target/produtos-service-0.0.1-SNAPSHOT.jar
java -jar fornecedores-service/target/fornecedores-service-0.0.1-SNAPSHOT.jar
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
java -jar gateway/target/gateway-0.0.1-SNAPSHOT.jar
```

O `application.properties` de fornecedores contém somente o nome e o endereço do Config Server.
Porta, banco e Eureka vêm de `config-repo/fornecedores-service.properties`.

### Conferência e prints

| Exercício | Evidência |
| --- | --- |
| 1 | `http://localhost:8761` com `PRODUTOS-SERVICE` registrado. |
| 2 | Pull Request de `atividade-PedroVieira` para `main` no próprio fork. |
| 3 | Terminal com `Started FornecedoresServiceApplication` e porta 8084. |
| 4 | `http://localhost:8084/h2-console`: JDBC `jdbc:h2:mem:fornecedoresdb`, usuário `sa`, senha vazia. Execute `SELECT * FROM fornecedor ORDER BY id;` antes do POST para mostrar cinco registros. |
| 5 | `GET http://localhost:8084/fornecedores` (200) e `GET http://localhost:8084/fornecedores/999999` (404). |
| 6 | Eureka com `FORNECEDORES-SERVICE` em estado `UP`. |
| 7 | `http://localhost:8888/fornecedores-service/default` e terminal com configuração obtida do Config Server e porta 8084. |
| 8 | `GET http://localhost:8085/fornecedores-service/fornecedores` com token de login. |
| 9 | POST abaixo retornando 201 e ID; depois repita o GET para conferir o cadastro. |
| 10 | `GET http://localhost:8084/fornecedores/produtos`, com produtos obtidos via Feign. |
| 11 | Containers em execução e GET pelo gateway após `docker compose up --build`. |
| 12 | Execução verde de **Build fornecedores-service** na aba Actions após o push. |

No Postman ou outro cliente HTTP, envie `POST http://localhost:8084/fornecedores`
com `Content-Type: application/json` e este corpo:

```json
{"nome":"Zeta Distribuidora","cnpj":"66.666.666/0001-91"}
```

O ID é gerado pelo banco; nome e CNPJ são obrigatórios e o CNPJ tem restrição de unicidade.
O H2 está em memória: reiniciar o serviço recria os cinco fornecedores iniciais.

### Autenticação do gateway

O gateway existente exige JWT. Cadastre um usuário com
`POST http://localhost:8085/auth-service/usuarios`:

```json
{"nome":"Aluno","email":"atividade@exemplo.com","senha":"Atividade123"}
```

Faça login com `POST http://localhost:8085/auth-service/usuarios/login`:

```json
{"email":"atividade@exemplo.com","senha":"Atividade123"}
```

Use o token retornado no cabeçalho `Authorization: Bearer SEU_TOKEN` ao consultar
`http://localhost:8085/fornecedores-service/fornecedores`.
As rotas são descobertas automaticamente no Eureka; não há rota manual para fornecedores.

### Docker e integração contínua

Encerre as aplicações Java locais antes de usar Docker, para liberar as portas.
Na raiz, execute `docker compose up --build`. O Compose aguarda o Config Server ficar
saudável e ativa o perfil `docker`; nele, o Eureka é acessado por `eureka-server:8761`.
Espere o registro dos serviços no Eureka antes de consultar o gateway.

O workflow `.github/workflows/fornecedores-service.yml` executa a cada push,
instala Java 17 e roda `mvn --batch-mode --no-transfer-progress -f fornecedores-service/pom.xml clean package`.
Deploy na Oracle Cloud não faz parte desta entrega.
