# Memory-Guard 

Aplicação Spring Boot para autenticação facial (webcam/imagem), gestão de credenciais (cofre criptografado) e automação de login via desktop, com auditoria e painel de administração.

## Principais funcionalidades

- Autenticação facial
  - Cadastrar (enroll) face a partir de imagem Base64
  - Autenticar via imagem Base64 ou webcam
  - Verificação 1:1 por usuário (`/auth/face/verify/{userId}`)
- Cofre de credenciais (Vault)
  - Salvar credenciais por `userId + system` com criptografia
- Automação de login (Desktop)
  - Abre URL e executa login digitando/colando usuário e senha (Java AWT Robot)
- Segurança do fluxo
  - Token temporário emitido após verificação facial para liberar ações sensíveis (cofre/automação)
- Auditoria
  - Registra eventos em `TBL_LOG_ATIVIDADE` (sucesso/falha + IP + máquina + data)
- Administração (UI)
  - Dashboard com métricas e logs recentes
  - CRUD de Config do sistema
  - CRUD de Dispositivos

## Dependências

- Java 17
- Spring Boot 4 (WebMVC, Security, Data JPA)
- MySQL (Connector/J)
- Lombok
- OpenCV (org.openpnp:opencv)

## Requisitos

- Java 17 instalado (JDK)
- Maven 3+
- MySQL rodando localmente (ou acessível via rede)
- Ambiente com GUI para automação (não headless) se for usar o Robot

## Como rodar (local)

### 1) Configurar ambiente

- Configure a conexão com o MySQL e os parâmetros de criptografia do cofre no seu ambiente local.
- Para automação de login, rode em um ambiente com interface gráfica (GUI) e sem modo headless.

### 2) Subir a aplicação

```bash
mvn clean spring-boot:run
```

Acesse:
- `http://localhost:8080/login.html`
- `http://localhost:8080/face.html`
- `http://localhost:8080/admin.html`

## Telas (UI)

- `login.html`
  - Captura foto, verifica face do usuário e redireciona para welcome
  - Login automático após validação facial (com token)
- `face.html`
  - Tira foto, cadastra face, autentica, salva credencial e dispara login automático
- `admin.html`
  - Dashboard + Logs
  - Config (CRUD)
  - Dispositivos (CRUD)

## Endpoints (resumo)

### Auth

- `POST /auth/face/enroll` (cadastrar face)
- `POST /auth/face/image` (autenticar por imagem)
- `POST /auth/face/verify/{userId}?system=...` (verificação 1:1)
  - Retorna `X-Auth-Token` e `X-Auth-Expires-At` em headers
- `POST /auth/login/{system}` (login automático)
  - Requer header `X-Auth-Token`
- `GET /auth/face/enroll/{userId}/image` (recupera foto cadastrada)
- `GET /auth/sessions/{userId}` (lista sessões biométricas recentes)
- `POST /auth/sessions/{userId}/end` (encerra sessões ativas)

### Vault

- `POST /vault/credential`
  - Requer header `X-Auth-Token`

### Robot

- `GET /robot/status`

### Admin

- `GET /admin/dashboard`
- `GET/PUT/DELETE /admin/config`
- `GET/POST/DELETE /admin/devices`

Se `MEMORYGUARD_ADMIN_KEY` estiver definido, enviar header `X-Admin-Key` nas rotas `/admin/**`.

## Banco de dados (tabelas principais)

- `TBL_PERFIL_USUARIO` (perfil + template/imagem facial)
- `TBL_SESSAO_BIOMETRICA` (sessões de autenticação: ACTIVE/ENDED/EXPIRED)
- `TBL_COFRE_SENHAS` (credenciais criptografadas por sistema)
- `TBL_CREDENCIAL_SESSAO` (token temporário pós-biometria)
- `TBL_LOG_ATIVIDADE` (auditoria)
- `TBL_CONFIGURACAO_SISTEMA` (config dinâmica)
- `TBL_DISPOSITIVO` (cadastro de dispositivos)

## Testes

```bash
mvn test
```

## Troubleshooting

- Port 8080 em uso:
  - Pare o processo que está usando a porta ou rode a aplicação em outra porta.
- Automação não funciona:
  - Verifique `SPRING_MAIN_HEADLESS=false` e se o ambiente tem GUI.
