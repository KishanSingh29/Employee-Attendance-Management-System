package com.attendance.leaveservice.exception;

/**
 * Raised when a leave request breaks a business rule: past dates, no working days
 * in the range, overlapping leave, insufficient balance, acting on a request that
 * is not PENDING, etc. Maps to HTTP 400.
 */
public class LeaveValidationException extends RuntimeException {

    public LeaveValidationException(String message) {
        super(message);
    }
}
