# Messaging Service

A REST API for one-to-one messaging between users. Supports sending messages, paginated conversation history, and listing a user's conversations — with participant-level access control.

---

## Architecture

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8+ |
| Build | Maven |

### Package Structure

```
com.subhash.messaging
├── controller/         # REST endpoints (MessagingController)
├── service/            # Business logic (ConversationService)
├── repository/         # JPA repositories
├── entity/             # JPA entities (DB tables)
├── dto/                # Request / response objects
├── exception/          # Custom exceptions + GlobalExceptionHandler
└── common/             # Shared utilities (PagedResponse, ErrorResponse, MessageMapper)
```

### Data Model

```
conversations
  id          BIGINT PK AUTO_INCREMENT
  created_at  DATETIME(6) NOT NULL

participants                                  -- join table: who is in which conversation
  id               BIGINT PK AUTO_INCREMENT
  conversation_id  BIGINT FK → conversations(id)
  user_id          BIGINT NOT NULL
  UNIQUE (conversation_id, user_id)           -- prevents duplicate membership

messages
  id               BIGINT PK AUTO_INCREMENT
  conversation_id  BIGINT FK → conversations(id)
  sender_id        BIGINT NOT NULL
  content          TEXT NOT NULL
  sent_at          DATETIME(6) NOT NULL
  INDEX (conversation_id, sent_at)            -- efficient paginated history
```

> There is no `users` table. User identity is a raw `Long` passed via the `X-User-Id` request header.

### How a Message Send Works

1. Caller sends `POST /api/v1/messages` with `X-User-Id: <senderId>` and `{ recipientId, content }`.
2. Service queries `participants` for a conversation that contains **both** users (GROUP BY / HAVING COUNT = 2).
3. If none exists, a new `conversations` row and two `participants` rows are created atomically.
4. A `messages` row is inserted and the response is returned.

### Access Control

Every read endpoint checks `participants` for `(conversationId, requesterId)` before returning data. Unknown or non-participant callers receive `403 Forbidden`.

### API Endpoints

| Method | Path | Header | Description |
|---|---|---|---|
| `POST` | `/api/v1/messages` | `X-User-Id` | Send a message (creates conversation if needed) |
| `GET` | `/api/v1/conversations` | `X-User-Id` | List all conversations for a user |
| `GET` | `/api/v1/conversations/{id}/messages` | `X-User-Id` | Get paginated message history |

---

## Running the Application

### Prerequisites

- Java 17+
- Maven (or use the included `./mvnw` wrapper)
- MySQL 8+ running locally

### 1. Create the database

```sql
CREATE DATABASE IF NOT EXISTS messaging_db;
```

> Or set `createDatabaseIfNotExist=true` (already in `application.yaml`) and ensure your MySQL user has `CREATE` privilege.

### 2. Configure credentials

The app reads credentials from environment variables with defaults:

| Variable | Default |
|---|---|
| `MYSQL_USER` | `root` |
| `MYSQL_PASSWORD` | `secret` |

**Option A — Terminal:**
```bash
export MYSQL_USER=root
export MYSQL_PASSWORD=yourpassword
./mvnw spring-boot:run
```

**Option B — IntelliJ IDEA:**
1. Run → Edit Configurations → select `MessagingServiceApplication`
2. Set **Environment variables**: `MYSQL_USER=root;MYSQL_PASSWORD=yourpassword`
3. Click OK and run

### 3. Start the application

```bash
./mvnw spring-boot:run
```

On first startup Hibernate runs DDL (`ddl-auto: update`) and creates the three tables automatically. The service listens on **port 8080**.

---

## Testing with curl

> All examples assume the service is running on `localhost:8080`.  
> Install `jq` for pretty-printed JSON output (optional but recommended).

### Send the first message (creates a new conversation)

```bash
curl -s -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"recipientId": 2, "content": "Hey, how are you?"}' | jq .
```

Expected: `201 Created`
```json
{
  "id": 1,
  "conversationId": 1,
  "senderId": 1,
  "content": "Hey, how are you?",
  "sentAt": "2024-06-24T10:00:00"
}
```

---

### Reply (reuses the same conversation)

```bash
curl -s -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{"recipientId": 1, "content": "I am good, thanks!"}' | jq .
```

Expected: `201 Created` with the **same `conversationId`** as above.

---

### List conversations for a user

```bash
curl -s http://localhost:8080/api/v1/conversations \
  -H "X-User-Id: 1" | jq .
```

Expected: `200 OK`
```json
[
  {
    "id": 1,
    "participantIds": [1, 2],
    "createdAt": "2024-06-24T10:00:00",
    "lastMessage": {
      "id": 2,
      "conversationId": 1,
      "senderId": 2,
      "content": "I am good, thanks!",
      "sentAt": "2024-06-24T10:01:00"
    }
  }
]
```

---

### Fetch paginated message history (authorized user)

```bash
curl -s "http://localhost:8080/api/v1/conversations/1/messages?page=0&size=10" \
  -H "X-User-Id: 1" | jq .
```

Expected: `200 OK`
```json
{
  "content": [
    { "id": 1, "conversationId": 1, "senderId": 1, "content": "Hey, how are you?", "sentAt": "..." },
    { "id": 2, "conversationId": 1, "senderId": 2, "content": "I am good, thanks!", "sentAt": "..." }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### Fetch second page of history

```bash
curl -s "http://localhost:8080/api/v1/conversations/1/messages?page=1&size=1" \
  -H "X-User-Id: 1" | jq .
```

---

### Access control — non-participant is rejected

```bash
curl -s "http://localhost:8080/api/v1/conversations/1/messages" \
  -H "X-User-Id: 99" | jq .
```

Expected: `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied to conversation 1",
  "timestamp": "..."
}
```

---

### Conversation not found

```bash
curl -s "http://localhost:8080/api/v1/conversations/999/messages" \
  -H "X-User-Id: 1" | jq .
```

Expected: `404 Not Found`
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Conversation not found with id: 999",
  "timestamp": "..."
}
```

---

### Validation error — missing content

```bash
curl -s -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"recipientId": 2}' | jq .
```

Expected: `400 Bad Request`
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "must not be blank",
  "timestamp": "..."
}
```

---

### Validation error — sending to yourself

```bash
curl -s -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"recipientId": 1, "content": "talking to myself"}' | jq .
```

Expected: `400 Bad Request`
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot send a message to yourself",
  "timestamp": "..."
}
```

---

### Missing X-User-Id header

```bash
curl -s http://localhost:8080/api/v1/conversations | jq .
```

Expected: `400 Bad Request`
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Required header missing: X-User-Id",
  "timestamp": "..."
}
```

---

### Start a separate conversation between different users

```bash
# User 3 messages User 4
curl -s -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 3" \
  -d '{"recipientId": 4, "content": "Hello from user 3"}' | jq .

# User 1 now has 1 conversation; User 3 has 1 conversation — they are separate
curl -s http://localhost:8080/api/v1/conversations -H "X-User-Id: 3" | jq .
```
