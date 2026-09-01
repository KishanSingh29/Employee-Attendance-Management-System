package com.attendance.attendanceservice.service;

import com.attendance.attendanceservice.dto.response.AttendanceResponse;
import com.attendance.attendanceservice.dto.response.AttendanceSummaryResponse;
import com.attendance.attendanceservice.dto.response.CheckInResponse;
import com.attendance.attendanceservice.dto.response.CheckOutResponse;
import com.attendance.attendanceservice.dto.response.EmployeeMonthlyReportResponse;
import com.attendance.attendanceservice.dto.response.HrAttendanceRecordResponse;
import com.attendance.attendanceservice.dto.response.HrDashboardResponse;
import com.attendance.attendanceservice.dto.response.TodayAttendanceResponse;
import com.attendance.attendanceservice.security.AuthenticatedUser;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    // ----- employee -----
    CheckInResponse checkIn(AuthenticatedUser user);

    CheckOutResponse checkOut(AuthenticatedUser user);

    TodayAttendanceResponse getToday(String userId);

    List<AttendanceResponse> getHistory(String userId, int month, int year);

    AttendanceSummaryResponse getSummary(String userId, int month, int year);

    // ----- HR -----
    List<HrAttendanceRecordResponse> getAllForDate(LocalDate date);

    List<AttendanceResponse> getEmployeeHistory(String userId, int month, int year);

    HrDashboardResponse getDashboard();

    List<EmployeeMonthlyReportResponse> getMonthlyReport(int month, int year);
}
