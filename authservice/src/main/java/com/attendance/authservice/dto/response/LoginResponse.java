package com.attendance.authservice.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String role,
        String employeeId,
        String firstName
) {
}
