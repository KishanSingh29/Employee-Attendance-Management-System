package com.attendance.attendanceservice.event;

/**
 * Consumed from the {@code user-registered} Kafka topic (published by authservice).
 * Local copy of the contract – there is no shared module. Deserialized from JSON.
 */
public record UserRegisteredEvent(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        String email,
        String department,
        String role
) {
}
