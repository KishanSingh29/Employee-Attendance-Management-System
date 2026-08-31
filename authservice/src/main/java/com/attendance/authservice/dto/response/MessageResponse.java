package com.attendance.authservice.dto.response;

public record MessageResponse(
        boolean success,
        String message
) {
}
