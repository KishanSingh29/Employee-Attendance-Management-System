package com.attendance.attendanceservice.dto.response;

import com.attendance.attendanceservice.entity.Attendance;
import com.attendance.attendanceservice.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A single attendance record as returned by history / employee-detail endpoints.
 */
public record AttendanceResponse(
        Long id,
        String userId,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkIn,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOut,
        Double workingHours,
        AttendanceStatus status,
        LocalDateTime createdAt
) {

    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(),
                a.getUserId(),
                a.getDate(),
                a.getCheckIn(),
                a.getCheckOut(),
                a.getWorkingHours(),
                a.getStatus(),
                a.getCreatedAt());
    }
}
