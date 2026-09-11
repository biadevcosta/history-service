package com.biadevcosta.history.domain.exception;

/** Base type for every business rule violation raised by the history domain. */
public class HistoryException extends RuntimeException {

    public HistoryException(String message) {
        super(message);
    }
}
