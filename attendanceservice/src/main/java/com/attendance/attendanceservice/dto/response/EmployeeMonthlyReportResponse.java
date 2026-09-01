package com.attendance.attendanceservice.dto.response;

/**
 * One employee's aggregated attendance for a month, used by the HR report.
 */
public record EmployeeMonthlyReportResponse(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        String department,
        long totalPresent,
        long totalAbsent,
        long totalLate,
        long totalHalfDay,
        long totalOnLeave,
        double totalWorkingHours,
        double averageHours
) {
}
