# tc-fiapx-auth-service

Microsservico de autenticacao do FIAP X (Fase 5). Responsavel por cadastro, login e emissao/validacao de JWT.

## Stack

- Java 21 + Spring Boot 4.1.2
- Spring Security 7 + JJWT 0.13 (HS256)
- PostgreSQL 16 + Flyway
- Springdoc OpenAPI 3.2
- Actuator, Micrometer, Prometheus, Zipkin
- JUnit 6, Mockito, AssertJ, JaCoCo 85%

## API

| Metodo | Caminho | Descricao |
|---|---|---|
| POST | `/api/auth/register` | Cadastra usuario (201) |
| POST | `/api/auth/login` | Autentica e devolve `accessToken` |
| GET | `/api/auth/me` | Dados do usuario autenticado |

Swagger: `http://localhost:8081/swagger-ui.html`

### Exemplo

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Maria","email":"maria@fiapx.com","senha":"senha1234"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@fiapx.com","senha":"senha1234"}'
```

## Configuracao

| Variavel | Padrao |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fiapx_auth` |
| `DB_USER` / `DB_PASSWORD` | `fiapx_auth_user` / `fiapx_auth_pass` |
| `JWT_SECRET` | segredo de desenvolvimento (>= 32 bytes) |
| `JWT_ISSUER` | `fiapx-auth` |
| `JWT_EXPIRATION` | `PT2H` |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` |

O `JWT_SECRET` deve ser identico ao configurado no `video-service`.

## Executar

```bash
mvn spring-boot:run
```

Ou com a infraestrutura completa:

```bash
cd .. && docker compose up --build -d auth-service
```

## Testes

```bash
mvn -B verify
```

37 testes e cobertura minima de 85% em linhas e branches.

## Docker

```bash
docker build -t tc-fiapx-auth-service:local .
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/fiapx_auth \
  -e DB_USER=fiapx_auth_user -e DB_PASSWORD=fiapx_auth_pass \
  -e JWT_SECRET=fiapx-prod-secret-key-with-at-least-32-bytes \
  tc-fiapx-auth-service:local
```

## Kubernetes

Manifests em `k8s/`: namespace, configmap, secret de exemplo, deployment, service, HPA e IngressRoute.

## Documentacao

Arquitetura completa, C4, banco, mensageria e roteiro de execucao: repositorio `tc-fiapx-docs`.
