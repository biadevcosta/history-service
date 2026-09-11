package com.biadevcosta.history.infrastructure.web.graphql;

import com.biadevcosta.history.application.EnrichedAppointment;
import com.biadevcosta.history.application.usecase.QueryHistoryUseCase;
import com.biadevcosta.history.domain.CallerContext;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL adapter. Builds {@link CallerContext} from the JWT so the use case decides ownership;
 * the {@code @PreAuthorize} here is only the coarse "must be one of these roles" gate.
 */
@Controller
public class HistoryQueryController {

    private final QueryHistoryUseCase queryHistoryUseCase;

    public HistoryQueryController(QueryHistoryUseCase queryHistoryUseCase) {
        this.queryHistoryUseCase = queryHistoryUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','PATIENT')")
    public List<HistoryItem> history(@Argument String patientId, @AuthenticationPrincipal Jwt jwt) {
        return queryHistoryUseCase.history(callerFrom(jwt), patientId).stream().map(HistoryItem::from).toList();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','PATIENT')")
    public List<HistoryItem> futureAppointments(@Argument String patientId, @AuthenticationPrincipal Jwt jwt) {
        return queryHistoryUseCase.future(callerFrom(jwt), patientId).stream().map(HistoryItem::from).toList();
    }

    private static CallerContext callerFrom(Jwt jwt) {
        return new CallerContext(jwt.getClaimAsString("role"), jwt.getClaimAsString("patientId"));
    }

    public record HistoryItem(String id, String patientName, String doctorName, String scheduledAt, String status) {
        static HistoryItem from(EnrichedAppointment a) {
            return new HistoryItem(a.appointmentId(), a.patientName(), a.doctorName(),
                    a.scheduledAt().toString(), a.status().name());
        }
    }
}
