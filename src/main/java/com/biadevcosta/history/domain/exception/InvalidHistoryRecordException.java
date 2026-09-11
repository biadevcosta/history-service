package com.biadevcosta.history.domain.exception;

/** Raised when an incoming appointment event cannot be turned into a valid {@code AppointmentRecord}. */
public class InvalidHistoryRecordException extends HistoryException {

    public InvalidHistoryRecordException(String message) {
        super(message);
    }
}
