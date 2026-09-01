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
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "Check in for today",
            description = "Marks the caller present. After 09:30 the status becomes LATE. A second check-in is rejected.")
    public ResponseEntity<CheckInResponse> checkIn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.checkIn(user));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Check out for today",
            description = "Requires an existing check-in. Under 4 worked hours the status becomes HALF_DAY.")
    public ResponseEntity<CheckOutResponse> checkOut(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.checkOut(user));
    }

    @GetMapping("/today")
    @Operation(summary = "Get the caller's attendance for today")
    public ResponseEntity<TodayAttendanceResponse> today(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getToday(userId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get the caller's attendance records for a month")
    public ResponseEntity<List<AttendanceResponse>> history(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId,
            @Parameter(example = "8") @RequestParam int month,
            @Parameter(example = "2026") @RequestParam int year) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getHistory(userId, month, year));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get the caller's monthly attendance summary")
    public ResponseEntity<AttendanceSummaryResponse> summary(
            @RequestHeader(value = USER_ID_HEADER, required = false) String xUserId,
            @Parameter(example = "8") @RequestParam int month,
            @Parameter(example = "2026") @RequestParam int year) {
        String userId = currentUserProvider.resolveTargetUserId(xUserId);
        return ResponseEntity.ok(attendanceService.getSummary(userId, month, year));
    }
}
