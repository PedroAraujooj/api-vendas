# Documentação Microserviços
https://claude.ai/code/artifact/2c541174-0b49-486d-9ee2-1682e91b8daf

# Documentação implementação Docker GUIA
https://claude.ai/code/artifact/a5f3c8e5-81de-4791-a16c-a71d4aa45f0f

# Documentação implementação Kubernetes GUIA
https://claude.ai/code/artifact/68cd2944-af3d-430f-8e6f-ea27c4186982

# Documentação deploy na Oracle Cloud GUIA
https://claude.ai/artifact/71fuEpCrAzrZzCS6kyVzKo

# Documentação GitHub Actions (CI/CD) GUIA
https://claude.ai/artifact/F21uNn1PE4FpN3ewMj6Aw6

Pedro Araujo Vieira - Matrícula: 16123288733
## Atividade: fornecedores-service

O novo serviço usa Java 17, Spring Boot, JPA/H2, Eureka, Config Server e OpenFeign.

### Execução com Docker

Na raiz do projeto:

```sh
docker compose up --build
```

Aguarde os serviços iniciarem e a descoberta do Eureka atualizar (pode levar cerca de um minuto). O Compose aguarda o Config Server ficar disponível antes de iniciar seus clientes.

- Eureka: http://localhost:8761
- Configuração: http://localhost:8888/fornecedores-service/default
- Fornecedores: http://localhost:8084/fornecedores
- Busca por ID: http://localhost:8084/fornecedores/1 (ID inexistente retorna 404)
- Produtos via Feign: http://localhost:8084/fornecedores/produtos
- Gateway: http://localhost:8085/fornecedores-service/fornecedores

O auth-service foi movido de 8084 para 8086 para liberar a porta exigida para fornecedores. O gateway mantém a autenticação JWT existente: cadastre um usuário em `POST http://localhost:8086/usuarios` com `nome`, `email` e `senha`; faça login em `POST http://localhost:8086/usuarios/login` com `email` e `senha`. Envie o token retornado no cabeçalho `Authorization: Bearer <token>` ao acessar o gateway. A rota é descoberta automaticamente pelo Eureka.

### Banco e cadastro

Abra http://localhost:8084/h2-console, use JDBC URL `jdbc:h2:mem:fornecedoresdb`, usuário `sa` e senha vazia. No perfil Docker, o console aceita a conexão do navegador e a porta 8084 é publicada somente no localhost da máquina. Execute:

```sql
SELECT * FROM FORNECEDOR ORDER BY ID;
```

Cinco fornecedores são cadastrados na inicialização. O banco fica em memória e é recriado quando o serviço reinicia. O print com cinco registros deve ser feito antes do POST.

Envie `POST http://localhost:8084/fornecedores` com `Content-Type: application/json`:

```json
{"nome":"Zeta Comercial","cnpj":"66777888000181"}
```

A resposta é 201 com o objeto e seu ID gerado. Nome e CNPJ são obrigatórios; o banco impede CNPJ duplicado.

### Execução local e compilação

Compile cada módulo com Java 17 e Maven (`mvn -f <modulo>/pom.xml package`). Inicie os JARs de `eureka-server` e `config-server` primeiro, depois `produtos-service`, `fornecedores-service`, `auth-service` e `gateway`, a partir da raiz do repositório. Exemplo para o novo serviço:

```sh
mvn -f fornecedores-service/pom.xml clean package
java -jar fornecedores-service/target/fornecedores-service-0.0.1-SNAPSHOT.jar
```

A porta 8084 vem exclusivamente do Config Server. `CONFIG_SERVER_URL` permite trocar o endereço do Config Server no Docker. O workflow `.github/workflows/fornecedores-service.yml` compila o serviço com Java 17 a cada push. Deploy na Oracle Cloud não faz parte desta entrega.
