package com.attendance.attendanceservice.controller;

import com.attendance.attendanceservice.dto.response.AttendanceResponse;
import com.attendance.attendanceservice.dto.response.AttendanceSummaryResponse;
import com.attendance.attendanceservice.dto.response.CheckInResponse;
import com.attendance.attendanceservice.dto.response.CheckOutResponse;
import com.attendance.attendanceservice.dto.response.TodayAttendanceResponse;
import com.attendance.attendanceservice.security.AuthenticatedUser;
import com.attendance.attendanceservice.security.CurrentUserProvider;
import com.attendance.attendanceservice.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/attendance")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Attendance", description = "Check-in / check-out and the caller's own attendance data")
public class AttendanceController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final AttendanceService attendanceService;
    private final CurrentUserProvider currentUserProvider;

    public AttendanceController(AttendanceService attendanceService,
                               CurrentUserProvider currentUserProvider) {
        this.attendanceService = attendanceService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/checkin")
    @Tag(name = "Attendance")
    @Operation(summary = "Check In",
            description = "Mark attendance. Before 9:30 AM = PRESENT, After 9:30 AM = LATE. Only one check-in per day allowed.")
    @SecurityRequirement(name = "Bearer Auth")
    @ApiResponse(responseCode = "200", description = "Checked in",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Checked in successfully",
              "checkInTime": "09:00:00",
              "status": "PRESENT"
            }
            """)))
    public ResponseEntity<CheckInResponse> checkIn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.checkIn(user));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Check Out",
            description = "Mark check-out. Working hours auto-calculated. Less than 4 hours = HALF_DAY.")
    @SecurityRequirement(name = "Bearer Auth")
    @ApiResponse(responseCode = "200", description = "Checked out",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Checked out successfully",
              "checkOutTime": "18:00:00",
              "workingHours": 9.0,
              "status": "PRESENT"
            }
            """)))
    public ResponseEntity<CheckOutResponse> checkOut(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.checkOut(user));
    }

    @GetMapping("/today")
    @Operation(summary = "Today's Attendance",
            description = "Get today's check-in/check-out and working hours.")
    @SecurityRequirement(name = "Bearer Auth")
    @ApiResponse(responseCode = "200", description = "Today's record",
            content = @Content(schema = @Schema(example = """
            {
              "date": "2026-09-01",
              "checkIn": "09:00:00",
              "checkOut": "18:00:00",
              "workingHours": 9.0,
              "status": "PRESENT",
              "checkedIn": true
            }
            """)))
    public ResponseEntity<TodayAttendanceResponse> today(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getToday(userId));
    }

    @GetMapping("/history")
    @Operation(summary = "Attendance History",
            description = "Monthly attendance records.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    public ResponseEntity<List<AttendanceResponse>> history(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId,
            @RequestParam int month,
            @RequestParam int year) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getHistory(userId, month, year));
    }

    @GetMapping("/summary")
    @Operation(summary = "Monthly Summary",
            description = "Total present, absent, late, half-day, working hours for the month.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    @ApiResponse(responseCode = "200", description = "Monthly summary",
            content = @Content(schema = @Schema(example = """
            {
              "month": 9,
              "year": 2026,
              "totalPresent": 20,
              "totalAbsent": 2,
              "totalLate": 3,
              "totalHalfDay": 1,
              "totalOnLeave": 1,
              "totalWorkingHours": 176.5,
              "averageHours": 8.4
            }
            """)))
    public ResponseEntity<AttendanceSummaryResponse> summary(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId,
            @RequestParam int month,
            @RequestParam int year) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getSummary(userId, month, year));
    }
}
