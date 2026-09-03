package com.attendance.attendanceservice.dto.response;

public record HrDashboardResponse(
        long totalEmployees,
        long presentToday,
        long absentToday,
        long onLeaveToday,
        long lateToday,
        long halfDayToday
) {
}
