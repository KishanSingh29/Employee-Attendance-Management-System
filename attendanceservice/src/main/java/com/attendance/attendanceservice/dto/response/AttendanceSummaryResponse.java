package com.attendance.attendanceservice.dto.response;

public record AttendanceSummaryResponse(
        int month,
        int year,
        long totalPresent,
        long totalAbsent,
        long totalLate,
        long totalHalfDay,
        long totalOnLeave,
        double totalWorkingHours,
        double averageHours
) {
}
