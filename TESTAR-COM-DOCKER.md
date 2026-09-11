# history-service — como testar quando o Docker estiver instalado

Checklist pra retomar. O **código está pronto**: 33 testes unit/component passando, ~97% de
cobertura, `./mvnw verify` em `BUILD SUCCESS`. Falta o que precisa de Docker de verdade: rodar o
`HistoryIntegrationTest` (Testcontainers) e ver o fluxo ponta a ponta com `scheduling` + `identity`
reais.

---

## 0. Pré-requisitos

- [ ] **Docker Desktop** instalado e **aberto** (o daemon rodando).
- [ ] **Temurin 21** + `JAVA_HOME` (ou o JDK embutido do VS Code).

---

## 1. Bateria de testes completa (com a integração)

```bash
cd history-service
./mvnw verify
```

**Esperado:** `BUILD SUCCESS`. Agora o `HistoryIntegrationTest` **roda de verdade** — sobe MySQL +
Kafka em containers e stuba `identity` com WireMock:

| Cenário | Esperado |
|---|---|
| publica `AppointmentCreated` | aparece em `history(patientId)`, com `patientName`/`doctorName` resolvidos |
| publica `AppointmentUpdated` pro mesmo `appointmentId` | o status muda, **sem** duplicar a linha |
| publica o **mesmo** `eventId` de novo (mesmo com payload diferente) | é ignorado — idempotência via `processed_events` |
| token `PATIENT` pede `history(patientId: "outro-id")` | recebe o **próprio** histórico, não o de `outro-id` |
| sem token | erro (401) |

Roda também o gate JaCoCo (mín. 80% — hoje ~97%).

---

## 2. Subir o serviço

```bash
cd history-service
docker compose up --build
```

Sobe: **`mysql-history`** (3308), **`kafka`** (9092), **`history`** (8083). `identity-service`
**não** está nesse compose — sobe separado e é alcançado em `http://host.docker.internal:8080`.

> Alternativa dev: `docker compose up -d mysql-history kafka` e `./mvnw spring-boot:run`.

---

## 3. O que mais precisa estar rodando (para um fluxo real ponta a ponta)

| Precisa | Por quê | Como |
|---|---|---|
| **`identity-service`** (+ MySQL) | resolve `patientName`/`doctorName` via `GET /users/{id}`; emite os tokens | `cd ../identity-service && docker compose up -d` |
| **`scheduling-service`** (+ MySQL + RabbitMQ + Kafka) | produz os eventos `AppointmentCreated`/`AppointmentUpdated` | `cd ../scheduling-service && docker compose up -d` — **ou** publicar na mão (passo 4) |

> `scheduling` e `history` precisam falar com o **mesmo** Kafka. Se subir os dois compose files
> juntos, aponte um dos dois pro Kafka do outro (edite `SPRING_KAFKA_BOOTSTRAP_SERVERS`), ou rode
> só o `history` e publique manualmente (passo 4).

---

## 4. Teste manual — ver o fluxo completo

1. Suba `identity` + `history` (e opcionalmente `scheduling`).
2. No `identity`, cadastre um paciente e um médico (`POST /users`, roles `PATIENT`/`DOCTOR`);
   anote os `id`.
3. Gere um evento — escolha um:
   - **Via `scheduling`:** logue como DOCTOR/NURSE, rode a mutation `scheduleAppointment` com
     esses ids.
   - **Na mão:** Kafka UI (se estiver usando o compose do `scheduling`, <http://localhost:8084>)
     ou `kafka-console-producer.sh` no tópico `appointment-events`, key = `patientId`, payload:
     ```json
     {
       "type": "AppointmentCreated",
       "eventId": "11111111-1111-1111-1111-111111111111",
       "appointmentId": "apt-1",
       "patientId": "<patient-id>",
       "doctorId": "<doctor-id>",
       "scheduledAt": "2030-12-01T10:00:00",
       "status": "SCHEDULED"
     }
     ```
4. Logue como o paciente (ou como o médico) e rode `history(patientId: "<patient-id>")` no
   GraphiQL (<http://localhost:8083/graphiql>) — a linha deve aparecer com nomes resolvidos.
5. Publique o **mesmo** `eventId` de novo → nenhuma duplicata aparece.
6. Publique um evento com `status: "CANCELLED"` e o **mesmo** `appointmentId` (novo `eventId`) →
   o status na consulta muda, ainda uma linha só.

---

## 5. Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `./mvnw verify` pula a integração | Docker Desktop não está aberto. |
| `history` sempre mostra `"Unknown"` como nome | `identity-service` fora do ar, ou `app.identity.base-url` errado (dentro do compose é `host.docker.internal:8080`). |
| Evento publicado não aparece | tópico/`bootstrap-servers` errado, ou `scheduling`/produtor manual apontando pra um Kafka diferente do que o `history` está consumindo. |
| 401 no GraphiQL | token ausente/expirado, ou emitido por um `issuer` diferente de `hospital-identity`. |

---

## 6. Arquivos importantes

| Arquivo | O quê |
|---|---|
| `plano-desenvolvimento.md` | roteiro (11 passos ✅) + §3 = as costuras da Clean Architecture |
| `README.md` | doc da solução: fluxo, o que é pluggável, o que sobe no Docker, dependências |
| `src/main/resources/application.yaml` | config (8083, datasource 3308, kafka consumer, `app.identity.*`) |
| `docker-compose.yml` | mysql-history + kafka + app |
