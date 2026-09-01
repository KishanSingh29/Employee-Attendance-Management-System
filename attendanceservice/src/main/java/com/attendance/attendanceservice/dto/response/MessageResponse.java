package com.attendance.attendanceservice.dto.response;

public record MessageResponse(
        boolean success,
        String message
) {
}
