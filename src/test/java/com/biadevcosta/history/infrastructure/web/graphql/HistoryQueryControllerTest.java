package com.biadevcosta.history.infrastructure.web.graphql;

import com.biadevcosta.history.application.EnrichedAppointment;
import com.biadevcosta.history.application.usecase.QueryHistoryUseCase;
import com.biadevcosta.history.domain.AppointmentStatus;
import com.biadevcosta.history.domain.CallerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryQueryControllerTest {

    @Mock
    QueryHistoryUseCase queryHistoryUseCase;

    private static Jwt jwtFor(String role, String patientId) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256")
                .subject("user-1").claim("role", role)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (patientId != null) {
            builder.claim("patientId", patientId);
        }
        return builder.build();
    }

    private static EnrichedAppointment sample() {
        return new EnrichedAppointment("apt-1", "John Doe", "Dr. Smith",
                LocalDateTime.of(2030, 9, 1, 14, 30), AppointmentStatus.SCHEDULED);
    }

    @Test
    void history_buildsCallerContextFromJwt_andMapsResult() {
        when(queryHistoryUseCase.history(any(), any())).thenReturn(List.of(sample()));
        var controller = new HistoryQueryController(queryHistoryUseCase);

        List<HistoryQueryController.HistoryItem> result =
                controller.history("pat-1", jwtFor("DOCTOR", null));

        ArgumentCaptor<CallerContext> callerCaptor = ArgumentCaptor.forClass(CallerContext.class);
        verify(queryHistoryUseCase).history(callerCaptor.capture(), org.mockito.ArgumentMatchers.eq("pat-1"));
        assertThat(callerCaptor.getValue().role()).isEqualTo("DOCTOR");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("apt-1");
        assertThat(result.getFirst().patientName()).isEqualTo("John Doe");
        assertThat(result.getFirst().status()).isEqualTo("SCHEDULED");
    }

    @Test
    void history_patientCaller_carriesPatientIdClaim() {
        when(queryHistoryUseCase.history(any(), any())).thenReturn(List.of());
        var controller = new HistoryQueryController(queryHistoryUseCase);

        controller.history(null, jwtFor("PATIENT", "pat-1"));

        ArgumentCaptor<CallerContext> callerCaptor = ArgumentCaptor.forClass(CallerContext.class);
        verify(queryHistoryUseCase).history(callerCaptor.capture(), org.mockito.ArgumentMatchers.isNull());
        assertThat(callerCaptor.getValue().role()).isEqualTo("PATIENT");
        assertThat(callerCaptor.getValue().patientId()).isEqualTo("pat-1");
    }

    @Test
    void futureAppointments_delegatesToUseCase() {
        when(queryHistoryUseCase.future(any(), any())).thenReturn(List.of(sample()));
        var controller = new HistoryQueryController(queryHistoryUseCase);

        List<HistoryQueryController.HistoryItem> result =
                controller.futureAppointments("pat-1", jwtFor("NURSE", null));

        verify(queryHistoryUseCase).future(any(), org.mockito.ArgumentMatchers.eq("pat-1"));
        assertThat(result).hasSize(1);
    }
}
