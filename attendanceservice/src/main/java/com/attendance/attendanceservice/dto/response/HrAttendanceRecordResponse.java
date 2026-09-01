package com.attendance.attendanceservice.dto.response;

import com.attendance.attendanceservice.entity.Attendance;
import com.attendance.attendanceservice.entity.AttendanceStatus;
import com.attendance.attendanceservice.entity.EmployeeProfile;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Attendance row enriched with employee identity for HR views.
 */
public record HrAttendanceRecordResponse(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        String department,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkIn,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOut,
        Double workingHours,
        AttendanceStatus status
) {

    public static HrAttendanceRecordResponse of(Attendance a, EmployeeProfile p) {
        return new HrAttendanceRecordResponse(
                a.getUserId(),
                p != null ? p.getEmployeeId() : null,
                p != null ? p.getFirstName() : null,
                p != null ? p.getLastName() : null,
                p != null ? p.getDepartment() : null,
                a.getDate(),
                a.getCheckIn(),
                a.getCheckOut(),
                a.getWorkingHours(),
                a.getStatus());
    }
}
