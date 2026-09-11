# history-service — Plano de pair programming

Read side (CQRS) do sistema. Consome os eventos do Kafka, monta um **modelo de leitura próprio** e
serve duas queries GraphQL. Construído **passo a passo** com Clean Architecture: nada em
`domain`/`application` conhece Kafka, `RestClient`, MySQL ou GraphQL.

> **Decisões (2026-09-06):**
> 1. Consumidor **Kafka** de `appointment-events`, `groupId = history` (fan-out — independente do `notification`).
> 2. Idempotência: tabela `processed_events` por `eventId`.
> 3. Nomes (`patientName`/`doctorName`) resolvidos **na query** via `identity` `GET /users/{id}` (cache).
> 4. Regra "future" (`SCHEDULED` + `scheduledAt` no futuro) é **domínio**, não SQL.
> 5. API **GraphQL** + resource server RS256 (igual ao `scheduling`).

---

## 1. Combinado de trabalho

Igual ao dos outros serviços (ver `../identity-service/plano-desenvolvimento.md` §1). Regra de ouro:
abra qualquer classe de `domain`/`application` — se aparecer `org.springframework`, `RestClient`,
`@KafkaListener`, `@Cacheable`, `com.mysql`, `ConsumerRecord`, GraphQL → vazou.

---

## 2. O que é o `history-service`

| | |
|---|---|
| Papel | **Read side (CQRS)**: consome eventos, monta o read model, serve `history` / `futureAppointments` |
| Porta | **8083** |
| Banco | `history_db` — `appointment_history` (read model) + `processed_events` (idempotência) |
| Entrada | Kafka `appointment-events` (key = `patientId`; payload `AppointmentEventMessage`) |
| Saída | GraphQL: `history(patientId)`, `futureAppointments(patientId)` — enriquecidas com nomes do `identity` |
| Segurança | resource server RS256 (`public.pem`); `@PreAuthorize` no resolver; "paciente só o seu" no use case |
| Pacote base | `com.biadevcosta.history` |

### Fluxo de ingestão

```
appointment-events ──▶ AppointmentEventListener (@KafkaListener groupId=history)
                         │ mapeia ConsumerRecord/AppointmentEventMessage -> RecordEventCommand
                         ▼
                       RecordAppointmentUseCase.apply(command)
                         1. processedEvents.exists(eventId)?  -> sim: retorna (idempotente)
                         2. historyRepository.upsert(record)     (Created insere, Updated atualiza; chave = appointmentId)
                         3. processedEvents.markProcessed(eventId)
```

### Fluxo de consulta

```
GraphQL query ──▶ HistoryQueryController (@PreAuthorize DOCTOR|NURSE|PATIENT)
                    │ monta CallerContext(role, patientId) a partir do JWT
                    ▼
                  QueryHistoryUseCase.history(caller, requestedPatientId)  /  .future(...)
                    1. effectivePatientId = role==PATIENT ? caller.patientId() : requestedPatientId
                    2. records = historyRepository.findByPatient(effectivePatientId)
                    3. future(): filtra records.stream().filter(r -> r.isFuture(now(clock)))
                    4. enrich: patientName/doctorName via userDirectory.findUserName(id) (cache; ausente -> "Unknown")
```

---

## 3. Clean Architecture — as costuras

| Costura | Porta (`application/port`) | Adapter (`infrastructure`) | Trocar por — sem tocar no core |
|---|---|---|---|
| Read model | `HistoryRepository` (`upsert`, `findByPatient`) | Spring Data JDBC + MySQL | outro store / view materializada |
| Idempotência | `ProcessedEventStore` (`exists`, `markProcessed`) | JDBC (`processed_events`) | Redis, memória |
| Nome dos usuários | `UserDirectory` (`Optional<String> findUserName(id)`) | `IdentityHttpUserDirectory` (`RestClient` + `@Cacheable`) | gRPC, stub |
| Transporte de entrada | *(adapter, não porta)* `AppointmentEventListener` | — | Kafka → outro bus: novo listener, mesmo use case (recebe `RecordEventCommand`, nunca `ConsumerRecord`) |
| Identidade do chamador | parâmetro `CallerContext(role, patientId)` no use case | montado no controller a partir do `Jwt` | — **nunca** `SecurityContextHolder` no core |
| Relógio | `java.time.Clock` no use case | `Clock.systemUTC()` | `Clock.fixed(...)` nos testes |

### `domain`

- `AppointmentStatus` — enum `SCHEDULED | COMPLETED | CANCELLED` (cópia própria).
- `AppointmentRecord` — `(appointmentId, patientId, doctorId, scheduledAt, status)` + `isFuture(LocalDateTime now)` = `status == SCHEDULED && scheduledAt.isAfter(now)`.
- `CallerContext` — `(String role, String patientId)`; helper `effectivePatientId(String requested)`.
- `exception/` — `HistoryException` (base).

### `application`

- `port/` — `HistoryRepository`, `ProcessedEventStore`, `UserDirectory`.
- `command/` — `RecordEventCommand(type, eventId, appointmentId, patientId, doctorId, scheduledAt, status)`.
- `usecase/` — `RecordAppointmentUseCase` (idempotente), `QueryHistoryUseCase` (`history` / `future`, ownership, enrich).
- `EnrichedAppointment` — record de saída `(id, patientName, doctorName, scheduledAt, status)` (o web mapeia pro `HistoryItem` do GraphQL).

### Anti-padrões que NÃO vamos cometer

| Vazamento | Correção |
|---|---|
| use case importar `AppointmentEventMessage` ou `ConsumerRecord` | `RecordEventCommand` com primitivos; o listener mapeia |
| filtro "future" em SQL (`WHERE scheduled_at > now`) | regra no `AppointmentRecord.isFuture(now)` (domínio) |
| use case ler `SecurityContextHolder` / `Jwt` | recebe `CallerContext` como parâmetro |
| `@Cacheable` na interface `UserDirectory` | no método do adapter |
| use case fazer `catch (HttpClientErrorException)` | adapter devolve `Optional.empty()`; use case decide o fallback "Unknown" |

---

## 4. Estrutura alvo

```
domain/           AppointmentStatus, AppointmentRecord (isFuture), CallerContext, exception/
application/
  port/           HistoryRepository, ProcessedEventStore, UserDirectory
  command/        RecordEventCommand
  usecase/        RecordAppointmentUseCase, QueryHistoryUseCase
  EnrichedAppointment
infrastructure/
  messaging/      AppointmentEventMessage (cópia), AppointmentEventListener (@KafkaListener), KafkaTopicConfig
  persistence/    AppointmentHistoryEntity/Repo/Mapper/HistoryRepositoryImpl (upsert via @Version/Persistable),
                  ProcessedEventEntity/Repo, JdbcProcessedEventStore
  client/         IdentityHttpUserDirectory (RestClient + @Cacheable)
  security/       SecurityConfig (resource server, claim role -> ROLE_)
  web/graphql/    HistoryQueryController, GraphQlExceptionResolver
  config/         UseCaseConfig, CacheConfig, IdentityClientProperties, JwtProperties

src/main/resources/
├── application.yaml
├── graphql/schema.graphqls
├── db/migration/V1__create_history.sql
└── public.pem     (validação do JWT — cópia de keys/public.pem)
```

---

## 5. Contrato Kafka e configuração

**Tópico** `appointment-events`, 3 partições, key = `patientId`. Payload (do `scheduling`):

```
type          "AppointmentCreated" | "AppointmentUpdated"
eventId       UUID único  (idempotência)
appointmentId, patientId, doctorId
scheduledAt, status
```

> O produtor manda com `spring.json.add.type.headers: true` (header `__TypeId__` com a FQN do
> `scheduling`). O consumidor será configurado com `spring.json.use.type.headers: false` +
> `spring.json.value.default.type = com.biadevcosta.history.infrastructure.messaging.AppointmentEventMessage`
> pra desserializar sempre no tipo local.

**`application.yaml`** (principais):

| Chave | Valor |
|---|---|
| `server.port` | `8083` |
| `spring.datasource.url` | `jdbc:mysql://localhost:3308/history_db` (root/root) |
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
| `spring.kafka.consumer.group-id` | `history` |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` |
| `spring.kafka.consumer.key-deserializer` / `value-deserializer` | `StringDeserializer` / `JsonDeserializer` |
| `spring.kafka.consumer.properties.spring.json.use.type.headers` | `false` |
| `spring.kafka.consumer.properties.spring.json.value.default.type` | `...history...AppointmentEventMessage` |
| `spring.kafka.consumer.properties.spring.json.trusted.packages` | `com.biadevcosta.*` |
| `security.jwt.public-key` | `classpath:public.pem` |
| `security.jwt.issuer` | `hospital-identity` |
| `app.identity.base-url` | `http://localhost:8080` |
| `spring.cache.*` | Caffeine, `user-names`, `maximumSize=2000,expireAfterWrite=10m` |

**`V1__create_history.sql`**:

```sql
CREATE TABLE appointment_history (
    appointment_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    version        BIGINT       NULL,
    patient_id     VARCHAR(36)  NOT NULL,
    doctor_id      VARCHAR(36)  NOT NULL,
    scheduled_at   DATETIME     NOT NULL,
    status         VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_history_patient ON appointment_history (patient_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) NOT NULL PRIMARY KEY,
    processed_at DATETIME    NOT NULL
);
```

---

## 6. Roteiro de passos

- [x] **0. Setup** — submódulo baixado; `pom.xml` reescrito (add `oauth2-resource-server`, `spring-kafka`, `cache`+`caffeine`, `actuator`, `spring-kafka-test`, Testcontainers `mysql`+`kafka`, WireMock, JaCoCo; testes consolidados); `docker-compose.yml` (mysql-history:3308 + kafka KRaft + app); `Dockerfile`; `public.pem` atualizado para `keys/public.pem`; `HistoryApplicationTests` `@Disabled`.
- [x] **1. Config + migration** — `application.yaml` (§5); `schema.graphqls`; `V1__create_history.sql`.
- [x] **2. Domínio** — `AppointmentStatus`, `AppointmentRecord` (`isFuture`), `CallerContext` (`effectivePatientId`) + testes.
- [x] **3. Portas + command** — `HistoryRepository`, `ProcessedEventStore`, `UserDirectory`, `RecordEventCommand`, `EnrichedAppointment`.
- [x] **4. `RecordAppointmentUseCase`** + teste: idempotência (`eventId` repetido não faz upsert); `Created` e `Updated` chamam `upsert`; ordem `exists → upsert → markProcessed`.
- [x] **5. `QueryHistoryUseCase`** + teste: PATIENT ignora o argumento (usa o do token); DOCTOR/NURSE usa o argumento; `future` filtra por `isFuture(now)`; enrich resolve nomes; nome ausente → "Unknown".
- [x] **6. Persistência** — `AppointmentHistoryEntity` (upsert via `@Version`), `HistoryRepositoryImpl`, `ProcessedEventEntity`/`JdbcProcessedEventStore` (insert-only). Testes Mockito.
- [x] **7. Messaging** — `AppointmentEventMessage` (cópia), `KafkaTopicConfig` (`NewTopic` 3 partições), `AppointmentEventListener` (`@KafkaListener` → command). Teste do mapeamento.
- [x] **8. `IdentityHttpUserDirectory`** + `CacheConfig` — `RestClient` + `@Cacheable("user-names")`; 404 → `Optional.empty()`. Teste `MockRestServiceServer`.
- [x] **9. Web + segurança** — `SecurityConfig` (resource server, `role`→`ROLE_`, `JwtValidators` com issuer); `HistoryQueryController` (`@QueryMapping` + `@PreAuthorize`, monta `CallerContext` do `Jwt`, mapeia `EnrichedAppointment`→`HistoryItem`); `GraphQlExceptionResolver`. Testes unit do controller.
- [x] **10. Wiring + IT** — `UseCaseConfig`; `SecurityTestConfig` (par RSA em memória); `HistoryIntegrationTest` (Testcontainers Kafka+MySQL + WireMock stub identity): publica `AppointmentCreated` → aparece em `history`; `AppointmentUpdated` → status muda; evento repetido → sem duplicata; PATIENT só vê o seu; DOCTOR vê qualquer um. Auto-pula sem Docker (verificado nesta máquina: `BUILD SUCCESS`, 33 testes unit/component, integração skip por assumption, cobertura ~97%).
- [x] **11. Fechamento** — `README.md` (solução, o que sobe no Docker, serviços externos, como testar), `TESTAR-COM-DOCKER.md`, JaCoCo, `Dockerfile` (feito).

---

## 7. Dependências de runtime

| Precisa | Por quê |
|---|---|
| **Kafka** | tópico de onde os eventos vêm |
| **MySQL** `history_db` | read model + idempotência |
| **`identity-service`** (+ MySQL) | `GET /users/{id}` para os nomes na query |
| **`scheduling-service`** (+ MySQL + Kafka) | produz os eventos ao criar/editar consulta — *ou* publica-se um evento de teste no tópico |
| um **JWT válido** assinado pela chave privada do `identity` | para chamar as queries (papel no claim `role`, `patientId` p/ paciente) |
