package com.biadevcosta.history.infrastructure.web.graphql;

import com.biadevcosta.history.domain.exception.InvalidHistoryRecordException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQlExceptionResolverTest {

    private final GraphQlExceptionResolver resolver = new GraphQlExceptionResolver();
    private final DataFetchingEnvironment env = DataFetchingEnvironmentImpl
            .newDataFetchingEnvironment().build();

    private GraphQLError resolve(Throwable ex) {
        return resolver.resolveToSingleError(ex, env);
    }

    @Test
    void invalidRecord_mapsToBadRequest() {
        GraphQLError error = resolve(new InvalidHistoryRecordException("bad"));
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getMessage()).isEqualTo("bad");
    }

    @Test
    void unknownException_isNotResolvedHere() {
        assertThat(resolve(new IllegalStateException("boom"))).isNull();
    }
}
