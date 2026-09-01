package com.attendance.attendanceservice.dto.response;

import com.attendance.attendanceservice.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record CheckOutResponse(
        boolean success,
        String message,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOutTime,
        Double workingHours,
        AttendanceStatus status
) {
}
