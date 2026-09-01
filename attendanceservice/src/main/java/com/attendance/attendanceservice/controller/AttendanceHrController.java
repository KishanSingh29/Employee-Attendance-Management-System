package com.attendance.attendanceservice.controller;

import com.attendance.attendanceservice.dto.response.AttendanceResponse;
import com.attendance.attendanceservice.dto.response.EmployeeMonthlyReportResponse;
import com.attendance.attendanceservice.dto.response.HrAttendanceRecordResponse;
import com.attendance.attendanceservice.dto.response.HrDashboardResponse;
import com.attendance.attendanceservice.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance/hr")
@PreAuthorize("hasRole('HR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance (HR)", description = "Organisation-wide attendance views, HR role only")
public class AttendanceHrController {

    private final AttendanceService attendanceService;

    public AttendanceHrController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/all")
    @Operation(summary = "All employees' attendance for a given date")
    public ResponseEntity<List<HrAttendanceRecordResponse>> allForDate(
            @Parameter(example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAllForDate(date));
    }

    @GetMapping("/employee/{userId}")
    @Operation(summary = "One employee's attendance history for a month")
    public ResponseEntity<List<AttendanceResponse>> employeeHistory(
            @PathVariable String userId,
            @Parameter(example = "8") @RequestParam int month,
            @Parameter(example = "2026") @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getEmployeeHistory(userId, month, year));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Today's attendance snapshot for the whole organisation")
    public ResponseEntity<HrDashboardResponse> dashboard() {
        return ResponseEntity.ok(attendanceService.getDashboard());
    }

    @GetMapping("/report")
    @Operation(summary = "Monthly attendance report for every employee")
    public ResponseEntity<List<EmployeeMonthlyReportResponse>> report(
            @Parameter(example = "8") @RequestParam int month,
            @Parameter(example = "2026") @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getMonthlyReport(month, year));
    }
}
