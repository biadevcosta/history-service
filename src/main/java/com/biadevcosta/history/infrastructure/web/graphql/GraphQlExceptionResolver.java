package com.biadevcosta.history.infrastructure.web.graphql;

import com.biadevcosta.history.domain.exception.InvalidHistoryRecordException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/** Maps domain exceptions to GraphQL error types instead of leaking a raw 500. */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType type = switch (ex) {
            case InvalidHistoryRecordException ignored -> ErrorType.BAD_REQUEST;
            default -> null;
        };
        if (type == null) {
            return null; // let Spring handle everything else
        }
        return GraphqlErrorBuilder.newError()
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }
}
