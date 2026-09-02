package com.attendance.leaveservice.exception;

/**
 * Raised when an authenticated caller tries to read or modify a leave request
 * that does not belong to them. Maps to HTTP 403.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
