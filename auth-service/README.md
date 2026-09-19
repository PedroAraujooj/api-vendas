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

## Como testar

Execute os exemplos no mesmo terminal PowerShell.

**1. Fazer login:**

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8085/auth-service/auth/login `
  -ContentType 'application/json' `
  -Body '{"username":"aluno","password":"Prova123!"}'

$login | Format-List
```

A resposta contém `access_token` e `refresh_token`.

**2. Acessar clientes com o token:**

```powershell
curl.exe -i http://localhost:8085/clientes-service/clientes `
  -H "Authorization: Bearer $($login.access_token)"
```

Resultado esperado: HTTP **200** e a lista de clientes.

**3. Renovar os tokens:**

```powershell
$novo = Invoke-RestMethod -Method Post -Uri http://localhost:8085/auth-service/auth/refresh `
  -ContentType 'application/json' `
  -Body (@{ refresh_token = $login.refresh_token } | ConvertTo-Json)

$novo | Format-List
```

Use `$novo.access_token` para acessar clientes e `$novo.refresh_token` na próxima renovação. O refresh anterior não pode ser reutilizado.

**4. Testar acesso sem autenticação:**

```powershell
curl.exe -i http://localhost:8085/clientes-service/clientes
```

Resultado esperado: HTTP **401**. Senha incorreta e tokens inválidos também são rejeitados com **401**.

## Observações

- O JWT dura cinco minutos e o refresh token dura 24 horas, por padrão.
- As chaves e os refresh tokens ficam em memória. Após reiniciar o auth-service, faça login novamente.
- As credenciais são apenas para demonstração.

Mais instruções estão no [README principal](../readme.md).
