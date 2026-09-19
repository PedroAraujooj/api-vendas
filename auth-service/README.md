# auth-service

Serviço independente de autenticação da API de vendas, feito com Java 17, Spring Boot e JWT. Ele valida o usuário e a senha e retorna tokens para acessar as rotas protegidas do serviço de clientes.

## Como executar

Com o Docker Desktop aberto, execute na raiz do projeto `api-vendas`:

```powershell
docker compose up --build -d
```

O auth-service fica na porta **8084** e se registra automaticamente no Eureka.
O Gateway, na porta **8085**, descobre o serviço e cria rotas com o prefixo `/auth-service`.
A descoberta pode levar cerca de um minuto após a inicialização.

Fora do Docker, o Eureka é acessado em `http://localhost:8761/eureka/`.
No Docker, a variável `EUREKA_URL` aponta para `http://eureka-server:8761/eureka/`.

**Usuário de teste:** `aluno`  
**Senha:** `Prova123!`

## Endpoints

| Método | Rota | Função |
| --- | --- | --- |
| POST | `/auth/login` | Autenticar e obter tokens |
| POST | `/auth/refresh` | Renovar os tokens |
| GET | `/auth/jwks` | Consultar a chave pública |
| GET | `/auth/health` | Verificar se o serviço está funcionando |

Essas rotas não exigem JWT no cabeçalho. O login exige usuário e senha válidos, e o refresh exige um refresh token válido.

As rotas da tabela são diretas, na porta 8084. Pelo Gateway, use
`/auth-service/auth/login` e `/auth-service/auth/refresh`.

A rota `GET /clientes-service/clientes`, no Gateway, é protegida e exige `Authorization: Bearer TOKEN`.

## Como testar com Postman

Com os serviços executando, crie as requisições abaixo no Postman e clique em **Send** para enviar. Os exemplos usam o Gateway na porta **8085**.

### 1. Acesso sem autenticação

- Método: **GET**
- URL: `http://localhost:8085/clientes-service/clientes`
- Em **Authorization**, selecione **No Auth**. Remova qualquer header `Authorization` adicionado manualmente.

Resultado esperado: **401 Unauthorized**.

### 2. Fazer login

- Método: **POST**
- URL: `http://localhost:8085/auth-service/auth/login`
- Em **Authorization**, selecione **No Auth**.
- Em **Body**, selecione **raw** e o formato **JSON**. Cole:

```json
{
  "username": "aluno",
  "password": "Prova123!"
}
```

Resultado esperado: **200 OK**. Copie os valores de `access_token` e `refresh_token` da resposta, sem as aspas.

### 3. Acessar clientes com o token

- Método: **GET**
- URL: `http://localhost:8085/clientes-service/clientes`
- Em **Authorization**, selecione **Bearer Token**.
- No campo **Token**, cole o `access_token` recebido no login, sem escrever `Bearer` antes dele.

Resultado esperado: **200 OK** e a lista de clientes.

### 4. Renovar os tokens

- Método: **POST**
- URL: `http://localhost:8085/auth-service/auth/refresh`
- Em **Authorization**, selecione **No Auth**.
- Em **Body → raw → JSON**, cole o corpo abaixo, substituindo o texto pelo `refresh_token` recebido no login:

```json
{
  "refresh_token": "COLE_AQUI_O_REFRESH_TOKEN"
}
```

Resultado esperado: **200 OK**, com novos valores de `access_token` e `refresh_token`.

### 5. Acessar clientes com o novo token

Volte à requisição de clientes do passo 3, substitua o campo **Token** pelo novo `access_token` e clique em **Send**.

Resultado esperado: **200 OK**. Na próxima renovação, use o novo `refresh_token`; o anterior já foi consumido.

### 6. Testar credenciais inválidas

- **Senha incorreta:** repita o login usando `"password": "senha-errada"`. Resultado: **401 Unauthorized**.
- **Token inválido:** na requisição de clientes, substitua o campo **Token** por `token-invalido`. Resultado: **401 Unauthorized**.
- **Refresh reutilizado:** envie novamente o refresh token consumido no passo 4. Resultado: **401 Unauthorized**.

Na porta 8085, mantenha os prefixos `/auth-service` e `/clientes-service` nas URLs. Os endereços antigos `/auth/login` e `/api/clientes` não estão configurados no Gateway.

## Observações

- O JWT dura cinco minutos e o refresh token dura 24 horas, por padrão.
- As chaves e os refresh tokens ficam em memória. Após reiniciar o auth-service, faça login novamente.
- As credenciais são apenas para demonstração.

Mais instruções estão no [README principal](../readme.md).
