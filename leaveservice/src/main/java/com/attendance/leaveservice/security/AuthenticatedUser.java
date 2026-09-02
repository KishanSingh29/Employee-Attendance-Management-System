package com.attendance.leaveservice.security;

/**
 * Immutable view of the caller, derived entirely from the verified JWT and kept
 * as the Spring Security principal.
 */
public record AuthenticatedUser(
        String userId,
        String employeeId,
        String email,
        String role
) {

    public boolean isHr() {
        return "HR".equalsIgnoreCase(role);
    }
}
