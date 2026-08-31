package com.attendance.authservice.event;

import com.attendance.authservice.entity.User;

/**
 * Published to the {@code user-registered} Kafka topic right after a new employee
 * is persisted. Downstream services (attendance, leave) use it to bootstrap their
 * own per-user rows. Serialized as JSON.
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

    public static UserRegisteredEvent from(User user) {
        return new UserRegisteredEvent(
                user.getUserId(),
                user.getEmployeeId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDepartment(),
                user.getRole() != null ? user.getRole().name() : null);
    }
}
