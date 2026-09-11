package com.biadevcosta.history.infrastructure.config;

import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.application.port.ProcessedEventStore;
import com.biadevcosta.history.application.port.UserDirectory;
import com.biadevcosta.history.application.usecase.QueryHistoryUseCase;
import com.biadevcosta.history.application.usecase.RecordAppointmentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the plain-POJO use cases as beans. Use cases carry no Spring annotations themselves. */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RecordAppointmentUseCase recordAppointmentUseCase(HistoryRepository historyRepository,
                                                      ProcessedEventStore processedEventStore,
                                                      Clock clock) {
        return new RecordAppointmentUseCase(historyRepository, processedEventStore, clock);
    }

    @Bean
    QueryHistoryUseCase queryHistoryUseCase(HistoryRepository historyRepository,
                                            UserDirectory userDirectory, Clock clock) {
        return new QueryHistoryUseCase(historyRepository, userDirectory, clock);
    }
}
