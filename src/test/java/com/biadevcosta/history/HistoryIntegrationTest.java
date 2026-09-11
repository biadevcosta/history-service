package com.biadevcosta.history;

import com.biadevcosta.history.infrastructure.messaging.AppointmentEventMessage;
import com.biadevcosta.history.support.AbstractIntegrationTest;
import com.biadevcosta.history.support.SecurityTestConfig;
import com.biadevcosta.history.support.TestTokens;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: real MySQL + Kafka containers, real security filter chain, GraphQL over HTTP;
 * identity-service is stubbed with WireMock. Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Import(SecurityTestConfig.class)
class HistoryIntegrationTest extends AbstractIntegrationTest {

    private static final WireMockServer WIREMOCK = new WireMockServer(options().dynamicPort());

    static {
        WIREMOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @DynamicPropertySource
    static void externalServices(DynamicPropertyRegistry registry) {
        registry.add("app.identity.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    HttpGraphQlTester graphQlTester;
    @Autowired
    TestTokens tokens;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${app.kafka.topic}")
    String topic;

    @BeforeEach
    void resetStubs() {
        WIREMOCK.resetAll();
        WIREMOCK.stubFor(get(urlEqualTo("/users/pat-1")).willReturn(okJson(
                "{\"id\":\"pat-1\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"role\":\"PATIENT\"}")));
        WIREMOCK.stubFor(get(urlEqualTo("/users/doc-1")).willReturn(okJson(
                "{\"id\":\"doc-1\",\"name\":\"Dr. Smith\",\"email\":\"smith@example.com\",\"role\":\"DOCTOR\"}")));
    }

    private GraphQlTester asDoctor(String doctorId) {
        return graphQlTester.mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.forUser(doctorId, "DOCTOR"))
                .build();
    }

    private GraphQlTester asPatient(String patientId) {
        return graphQlTester.mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.forPatient(patientId, "PATIENT", patientId))
                .build();
    }

    private void publish(String type, String eventId, String appointmentId, String patientId,
                         String doctorId, LocalDateTime scheduledAt, String status) {
        kafkaTemplate.send(topic, patientId, new AppointmentEventMessage(
                type, eventId, appointmentId, patientId, doctorId, scheduledAt, status));
    }

    private List<Object> pollHistory(GraphQlTester caller, String query, String path) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            List<Object> result = caller.document(query).execute().path(path).entityList(Object.class).get();
            if (!result.isEmpty()) {
                return result;
            }
            Thread.sleep(100);
        }
        return List.of();
    }

    @Test
    void createdEvent_appearsInHistory_enrichedWithNames() throws InterruptedException {
        publish("AppointmentCreated", UUID.randomUUID().toString(), "apt-1", "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 1, 10, 0), "SCHEDULED");

        GraphQlTester doctor = asDoctor("doc-1");
        List<Object> items = pollHistory(doctor, """
                query {
                  history(patientId: "pat-1") { id patientName doctorName status }
                }
                """, "history");

        assertThat(items).hasSize(1);
        doctor.document("""
                        query {
                          history(patientId: "pat-1") { patientName doctorName status }
                        }
                        """)
                .execute()
                .path("history[0].patientName").entity(String.class).isEqualTo("John Doe")
                .path("history[0].doctorName").entity(String.class).isEqualTo("Dr. Smith")
                .path("history[0].status").entity(String.class).isEqualTo("SCHEDULED");
    }

    @Test
    void updatedEvent_changesStatus_withoutDuplicatingTheRow() throws InterruptedException {
        String appointmentId = "apt-2";
        publish("AppointmentCreated", UUID.randomUUID().toString(), appointmentId, "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 2, 10, 0), "SCHEDULED");
        GraphQlTester doctor = asDoctor("doc-1");
        pollHistory(doctor, """
                query { history(patientId: "pat-1") { id status } }
                """, "history");

        publish("AppointmentUpdated", UUID.randomUUID().toString(), appointmentId, "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 2, 10, 0), "CANCELLED");

        for (int i = 0; i < 100; i++) {
            List<String> statuses = doctor.document("""
                            query { history(patientId: "pat-1") { id status } }
                            """)
                    .execute()
                    .path("history[*].status").entityList(String.class).get();
            if (statuses.contains("CANCELLED")) {
                break;
            }
            Thread.sleep(100);
        }

        doctor.document("""
                        query { history(patientId: "pat-1") { id status } }
                        """)
                .execute()
                .path("history").entityList(Object.class)
                .satisfies(list -> assertThat(list).hasSize(1))
                .path("history[0].status").entity(String.class).isEqualTo("CANCELLED");
    }

    @Test
    void repeatedEventId_isIgnored_noDuplicateRow() throws InterruptedException {
        String appointmentId = "apt-3";
        String eventId = UUID.randomUUID().toString();
        publish("AppointmentCreated", eventId, appointmentId, "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 3, 10, 0), "SCHEDULED");
        GraphQlTester doctor = asDoctor("doc-1");
        pollHistory(doctor, """
                query { history(patientId: "pat-1") { id status } }
                """, "history");

        // same eventId, different status — must be dropped as a redelivery, not applied
        publish("AppointmentCreated", eventId, appointmentId, "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 3, 10, 0), "CANCELLED");
        Thread.sleep(1_000);

        doctor.document("""
                        query { history(patientId: "pat-1") { id status } }
                        """)
                .execute()
                .path("history[0].status").entity(String.class).isEqualTo("SCHEDULED");
    }

    @Test
    void patientCaller_onlySeesOwnHistory_ignoringRequestedId() throws InterruptedException {
        publish("AppointmentCreated", UUID.randomUUID().toString(), "apt-4", "pat-1", "doc-1",
                LocalDateTime.of(2030, 12, 4, 10, 0), "SCHEDULED");
        pollHistory(asDoctor("doc-1"), """
                query { history(patientId: "pat-1") { id status } }
                """, "history");

        asPatient("pat-1").document("""
                        query {
                          history(patientId: "someone-else") { id patientName }
                        }
                        """)
                .execute()
                .path("history[0].patientName").entity(String.class).isEqualTo("John Doe");
    }

    @Test
    void missingToken_isUnauthorized() {
        graphQlTester.document("""
                        query { history(patientId: "pat-1") { id } }
                        """)
                .execute()
                .errors()
                .expect(error -> error.getMessage() != null);
    }
}
