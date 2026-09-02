package com.attendance.leaveservice.dto.response;

public record MessageResponse(
        boolean success,
        String message
) {
}
