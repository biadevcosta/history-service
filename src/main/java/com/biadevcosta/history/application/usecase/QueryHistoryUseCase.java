package com.biadevcosta.history.application.usecase;

import com.biadevcosta.history.application.EnrichedAppointment;
import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.application.port.UserDirectory;
import com.biadevcosta.history.domain.AppointmentRecord;
import com.biadevcosta.history.domain.CallerContext;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serves the two read queries. Ownership ("patient sees only own") is enforced by
 * {@link CallerContext#effectivePatientId}, never by rejecting the call.
 */
public class QueryHistoryUseCase {

    private static final String UNKNOWN = "Unknown";

    private final HistoryRepository historyRepository;
    private final UserDirectory userDirectory;
    private final Clock clock;

    public QueryHistoryUseCase(HistoryRepository historyRepository, UserDirectory userDirectory, Clock clock) {
        this.historyRepository = historyRepository;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    public List<EnrichedAppointment> history(CallerContext caller, String requestedPatientId) {
        return recordsFor(caller, requestedPatientId).stream().map(this::enrich).toList();
    }

    public List<EnrichedAppointment> future(CallerContext caller, String requestedPatientId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return recordsFor(caller, requestedPatientId).stream()
                .filter(record -> record.isFuture(now))
                .map(this::enrich)
                .toList();
    }

    private List<AppointmentRecord> recordsFor(CallerContext caller, String requestedPatientId) {
        String effectivePatientId = caller.effectivePatientId(requestedPatientId);
        return historyRepository.findByPatient(effectivePatientId);
    }

    private EnrichedAppointment enrich(AppointmentRecord record) {
        String patientName = userDirectory.findUserName(record.patientId()).orElse(UNKNOWN);
        String doctorName = userDirectory.findUserName(record.doctorId()).orElse(UNKNOWN);
        return new EnrichedAppointment(record.appointmentId(), patientName, doctorName,
                record.scheduledAt(), record.status());
    }
}
