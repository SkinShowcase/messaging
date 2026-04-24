# Messaging (Skins Showcase)

Сервис личных сообщений: REST + WebSocket + TCP push. Использует **тот же JWT секрет**, что и `auth` (HS256).

Репозиторий: https://github.com/SkinShowcase/messaging  
Инфраструктура (compose): https://github.com/SkinShowcase/infrastructure

## Порты

- HTTP: **8082** (`server.port`)
- TCP notifications: **9090** (`MESSAGING_TCP_PORT` / `messaging.tcp.port`)

## Авторизация

- Почти все `GET/POST/DELETE` под `/api/**` требуют `Authorization: Bearer <JWT>`.
- Исключения: админские эндпоинты — по `X-Admin-Api-Key`.

## REST API (актуально)

Источник: `MessagingController`.

- `POST /api/chats/{recipientSteamId}/messages` — отправить сообщение (`{"text":"..."}`)
- `GET /api/chats/{counterpartySteamId}/messages?page=&size=` — история (size ≤ 100)
- `DELETE /api/chats/{counterpartySteamId}/messages/{messageId}` — удалить **только участникам чата** и только если сообщению **< 24 часов** (`409` иначе)
- `GET /api/chats` — список чатов (превью строится из расшифрованного текста)
- `GET /api/chats/by-username/{username}` — найти собеседника через `auth` (`/auth/users/by-username/...`)

### Поддержка (синтетический SteamID)

В чат-листе всегда присутствует «чат с поддержкой». Отправитель/получатель поддержки — фиксированный SteamID64 из `SupportSyntheticSteamId` (см. код).

## Админ API

Источник: `AdminSupportController` (`/api/admin/messaging/**`), заголовок `X-Admin-Api-Key`.

- `GET /api/admin/messaging/support/incoming?page=&size=`
- `POST /api/admin/messaging/support/messages` — тело `{"recipientSteamId":"765…","text":"..."}`

## WebSocket

- Endpoint в сервисе: `/ws/messages` (через gateway — см. `api-gateway` маршрут `/ws/messages/**`)
- Аутентификация: query `?token=<JWT>` (см. `MessagingWebSocketHandler`)

`MESSAGING_REQUIRE_SECURE_TRANSPORT` (см. `application.yml` + docker env): в проде обычно требуется `wss://`, локально можно ослабить.

## TCP уведомления

Клиент подключается к TCP порту messaging (по умолчанию 9090):

1. Первая строка: JWT
2. Ответ: `OK <steamId>` или `ERROR …`
3. События: `NEW_MESSAGE <senderSteamId> <messageId>`
4. `PING` → `PONG`

Через публичный интернет обычно используют TCP на **gateway** (он проксирует в messaging).

## Хранение данных: шифрование и идентификаторы

Реализация: `MessagingService` + `MessageCryptoService` + `SteamIdHashingService`.

- Текст сообщения в БД: **AES-GCM**, ключ `MESSAGING_MESSAGE_CRYPTO_KEY` — **base64 32 байта** (см. `MessageCryptoService`).
- Для индексации/поиска чатов в колонках `sender_id` / `recipient_id` хранится **SHA-256 hex** SteamID64.
- Дополнительно есть `sender_id_enc` / `recipient_id_enc` — **AES-GCM** от канонического SteamID64 строкой (миграция `V13__...`).
- API ответы (`MessageResponseDto`) отдают **реальные SteamID64** и plaintext `text` (как в момент отправки).

Старые строки без шифрования текста: `MessageCryptoService.decrypt` падает назад на «как есть» для совместимости.

## Зависимости

- `AUTH_SERVICE_BASE_URL` + JWT secret (`AUTH_JWT_SECRET` / yaml `app.auth.jwt.secret`) — валидация токена
- Для списка чатов дергается `auth`: batch пресетов (`POST /auth/users/preset-avatar-ids`)

## Запуск локально

```bash
./gradlew bootRun
# или:
./gradlew bootRun --args="--spring.profiles.active=local"
```

## Docker

```bash
docker build -t skins-showcase/messaging .
docker run --rm -p 8082:8082 -p 9090:9090 -e SERVER_PORT=8082 -e SPRING_PROFILES_ACTIVE=docker skins-showcase/messaging
```
