package com.attendance.authservice.dto.response;

public record RegisterResponse(
        boolean success,
        String message,
        String employeeId,
        String userId
) {
}
