package com.attendance.attendanceservice.exception;

/**
 * Raised when a check-in / check-out request violates a business rule
 * (e.g. checking in twice, checking out before checking in). Maps to HTTP 400.
 */
public class AttendanceException extends RuntimeException {

    public AttendanceException(String message) {
        super(message);
    }
}
