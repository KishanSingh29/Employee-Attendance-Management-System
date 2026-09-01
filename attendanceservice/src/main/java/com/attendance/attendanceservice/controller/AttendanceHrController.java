package com.attendance.attendanceservice.controller;

import com.attendance.attendanceservice.dto.response.AttendanceResponse;
import com.attendance.attendanceservice.dto.response.EmployeeMonthlyReportResponse;
import com.attendance.attendanceservice.dto.response.HrAttendanceRecordResponse;
import com.attendance.attendanceservice.dto.response.HrDashboardResponse;
import com.attendance.attendanceservice.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "HR - Attendance", description = "Organisation-wide attendance views, HR role only")
public class AttendanceHrController {

    private final AttendanceService attendanceService;

    public AttendanceHrController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/all")
    @Operation(summary = "All Employees Attendance",
            description = "HR only. All employees attendance for specific date.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "date", in = ParameterIn.QUERY,
            example = "2026-09-01",
            description = "Date in yyyy-MM-dd format")
    public ResponseEntity<List<HrAttendanceRecordResponse>> allForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAllForDate(date));
    }

    @GetMapping("/employee/{userId}")
    @Operation(summary = "Employee Attendance History",
            description = "HR only. Specific employee monthly attendance.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "userId", in = ParameterIn.PATH,
            example = "765a2d44-6423-47d9-a542-69e58d028a4d")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    public ResponseEntity<List<AttendanceResponse>> employeeHistory(
            @PathVariable String userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getEmployeeHistory(userId, month, year));
    }

    @GetMapping("/dashboard")
    @Tag(name = "HR - Attendance")
    @Operation(summary = "HR Dashboard",
            description = "HR only. Today's attendance overview.")
    @SecurityRequirement(name = "Bearer Auth")
    @ApiResponse(responseCode = "200", description = "Dashboard data",
            content = @Content(schema = @Schema(example = """
            {
              "totalEmployees": 10,
              "presentToday": 7,
              "absentToday": 2,
              "onLeaveToday": 1,
              "lateToday": 3
            }
            """)))
    public ResponseEntity<HrDashboardResponse> dashboard() {
        return ResponseEntity.ok(attendanceService.getDashboard());
    }

    @GetMapping("/report")
    @Operation(summary = "Monthly Report",
            description = "HR only. All employees monthly attendance report.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    public ResponseEntity<List<EmployeeMonthlyReportResponse>> report(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getMonthlyReport(month, year));
    }
}
