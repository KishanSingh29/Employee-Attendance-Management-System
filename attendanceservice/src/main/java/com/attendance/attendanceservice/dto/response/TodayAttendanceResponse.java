package com.attendance.attendanceservice.dto.response;

import com.attendance.attendanceservice.entity.Attendance;
import com.attendance.attendanceservice.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record TodayAttendanceResponse(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkIn,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOut,
        Double workingHours,
        String workingHoursDisplay,
        AttendanceStatus status,
        boolean checkedIn
) {

    public static TodayAttendanceResponse from(Attendance a) {
        boolean inProgress = a.getCheckIn() != null && a.getCheckOut() == null;
        String display = inProgress
                ? "In Progress"
                : (a.getWorkingHours() != null ? String.valueOf(a.getWorkingHours()) : null);
        return new TodayAttendanceResponse(
                a.getDate(),
                a.getCheckIn(),
                a.getCheckOut(),
                a.getWorkingHours(),
                display,
                a.getStatus(),
                a.getCheckIn() != null);
    }

    public static TodayAttendanceResponse notCheckedIn(LocalDate date) {
        return new TodayAttendanceResponse(date, null, null, null, null, AttendanceStatus.ABSENT, false);
    }
}
