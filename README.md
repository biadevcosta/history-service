# history-service

Read side (CQRS) of the hospital appointment system. Consumes `appointment-events` from
scheduling-service, builds its own read model, and serves two GraphQL queries: `history` and
`futureAppointments`, enriched with patient/doctor names resolved from identity-service.

Port: **8083**.

## Architecture (Clean Architecture)

```
domain/          AppointmentRecord (isFuture), CallerContext (effectivePatientId), no framework imports
application/     use cases as POJOs + ports (interfaces) + command
  usecase/RecordAppointmentUseCase, QueryHistoryUseCase
  port/HistoryRepository, ProcessedEventStore, UserDirectory
  command/RecordEventCommand, EnrichedAppointment
infrastructure/  everything Spring
  messaging/     AppointmentEventListener (@KafkaListener) + KafkaTopicConfig
  persistence/   Spring Data JDBC entities + mappers + HistoryRepositoryImpl, JdbcProcessedEventStore
  client/        IdentityHttpUserDirectory (RestClient + @Cacheable)
  security/      SecurityConfig (JWT resource server, issuer validation, role -> ROLE_ authority)
  web/graphql/   HistoryQueryController + GraphQlExceptionResolver
  config/        UseCaseConfig, CacheConfig, IdentityClientProperties, JwtProperties
```

### Ingestion flow

```
appointment-events ──▶ AppointmentEventListener (@KafkaListener, groupId=history)
                         1. processedEvents.exists(eventId)? -> yes: no-op (idempotent)
                         2. historyRepository.upsert(record)   (Created and Updated both upsert; key = appointmentId)
                         3. processedEvents.markProcessed(eventId)
```

### Query flow

```
GraphQL query ──▶ HistoryQueryController (@PreAuthorize DOCTOR|NURSE|PATIENT)
                    1. effectivePatientId = role==PATIENT ? caller.patientId() : requested
                    2. records = historyRepository.findByPatient(effectivePatientId)
                    3. future(): filters records by AppointmentRecord.isFuture(now)
                    4. enrich: patientName/doctorName via userDirectory.findUserName(id); absent -> "Unknown"
```

Rule enforcement:

| Rule | Where |
|---|---|
| Redelivered Kafka event is a no-op | `RecordAppointmentUseCase` (checks `processedEvents` first) |
| "future" = SCHEDULED + date ahead of now | `AppointmentRecord.isFuture(now)` — domain, not SQL |
| A PATIENT token only ever sees its own history | `CallerContext.effectivePatientId` — overridden, not rejected |
| Missing user name never breaks a query | `IdentityHttpUserDirectory` returns `Optional.empty()`; the use case falls back to `"Unknown"` |

## Configuration (`src/main/resources/application.yaml`)

| Key | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3308/history_db` | Flyway migrates `V1__create_history.sql` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | consumer group `history`, `auto-offset-reset: earliest` |
| `spring.kafka.consumer.properties.spring.json.*` | see yaml | always deserializes into the local `AppointmentEventMessage`, ignoring `__TypeId__` headers |
| `spring.cache.*` | Caffeine, `user-names`, `maximumSize=2000,expireAfterWrite=10m` | |
| `security.jwt.public-key` / `issuer` | `classpath:public.pem` / `hospital-identity` | RS256 public key, shared with `identity-service` / `scheduling-service` |
| `app.identity.base-url` | `http://localhost:8080` | for `GET /users/{id}` |
| `app.kafka.topic` | `appointment-events` | |

## Run locally

Everything in containers (builds the image, waits for MySQL / Kafka to be healthy):

```bash
docker compose up --build
```

That starts `mysql-history` (host **3308**), `kafka` (host **9092**), **`history`**
(host **8083**). `identity-service` is **not** part of this compose file — it runs standalone and
is reached at `http://host.docker.internal:8080` from inside the container.

Or backing services only, app on the host:

```bash
docker compose up -d mysql-history kafka
./mvnw spring-boot:run
```

## API docs (GraphQL)

GraphiQL is the interactive explorer (the GraphQL analog of Swagger UI):

| URL | What |
|---|---|
| <http://localhost:8083/graphiql> | GraphiQL explorer. Open **Headers** and add `{"Authorization":"Bearer <token>"}` (token from `identity-service` `POST /auth/login`), then run a query. |
| <http://localhost:8083/graphql/schema> | the SDL schema as text |
| `POST http://localhost:8083/graphql` | the endpoint itself (JWT required — `DOCTOR`, `NURSE` or `PATIENT`) |

```graphql
query {
  history(patientId: "pat-1") {
    id patientName doctorName scheduledAt status
  }
}
```

```graphql
query {
  futureAppointments(patientId: "pat-1") {
    id patientName doctorName scheduledAt status
  }
}
```

A `PATIENT` token ignores whatever `patientId` is passed and always sees its own history (the
token's `patientId` claim wins).

## Tests

```bash
./mvnw test         # unit tests (domain + use cases + adapters), no Docker needed
./mvnw verify        # + HistoryIntegrationTest (Testcontainers: MySQL/Kafka + WireMock stub for identity) + JaCoCo 80% gate
```

`HistoryIntegrationTest` needs a running Docker daemon; it self-skips (assumption failure) when
none is reachable. It signs its own RS256 tokens with an in-memory keypair
(`support/SecurityTestConfig`) and drives the real GraphQL + security stack, publishing real
`AppointmentEventMessage`s onto a Testcontainers Kafka broker.

Coverage report: `target/site/jacoco/index.html`. `*Application`, `infrastructure/config/**` and
`infrastructure/security/SecurityConfig` are excluded from the 80% line gate.

## Build image

```bash
docker build -t history-service .
```
