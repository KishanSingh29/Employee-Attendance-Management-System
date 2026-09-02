package com.attendance.leaveservice.controller;

import com.attendance.leaveservice.dto.request.RejectLeaveRequest;
import com.attendance.leaveservice.dto.request.UpdateSalaryRequest;
import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.dto.response.LeaveDecisionResponse;
import com.attendance.leaveservice.dto.response.LeaveReportRowResponse;
import com.attendance.leaveservice.dto.response.LeaveRequestResponse;
import com.attendance.leaveservice.dto.response.MessageResponse;
import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.security.AuthenticatedUser;
import com.attendance.leaveservice.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leave/hr")
@PreAuthorize("hasRole('HR')")
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "HR - Leave", description = "Approve / reject requests and view organisation-wide leave data, HR role only")
public class LeaveHrController {

    private final LeaveService leaveService;

    public LeaveHrController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/pending")
    @Tag(name = "HR - Leave")
    @Operation(summary = "Pending Leave Requests",
            description = "HR only. All pending leave requests.")
    @SecurityRequirement(name = "Bearer Auth")
    public ResponseEntity<List<LeaveRequestResponse>> pending() {
        return ResponseEntity.ok(leaveService.pending());
    }

    @PutMapping("/approve/{leaveId}")
    @Operation(summary = "Approve Leave",
            description = "HR only. Approve leave. Balance deducted automatically.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "leaveId", in = ParameterIn.PATH, example = "1")
    @ApiResponse(responseCode = "200", description = "Leave approved",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Leave approved",
              "leaveId": 1,
              "status": "APPROVED"
            }
            """)))
    public ResponseEntity<LeaveDecisionResponse> approve(
            @AuthenticationPrincipal AuthenticatedUser hr,
            @PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.approve(hr, leaveId));
    }

    @PutMapping("/reject/{leaveId}")
    @Operation(summary = "Reject Leave",
            description = "HR only. Reject leave with reason.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "leaveId", in = ParameterIn.PATH, example = "1")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "reason": "Project deadline"
            }
            """)))
    public ResponseEntity<LeaveDecisionResponse> reject(
            @AuthenticationPrincipal AuthenticatedUser hr,
            @PathVariable Long leaveId,
            @Valid @RequestBody RejectLeaveRequest request) {
        return ResponseEntity.ok(leaveService.reject(hr, leaveId, request.reason()));
    }

    @GetMapping("/all")
    @Operation(summary = "All Leave Requests",
            description = "HR only. All requests. Filter by status: PENDING/APPROVED/REJECTED.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "status", in = ParameterIn.QUERY,
            example = "PENDING",
            description = "Optional: PENDING, APPROVED, REJECTED")
    public ResponseEntity<List<LeaveRequestResponse>> all(
            @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(leaveService.all(status));
    }

    @GetMapping("/employee/{userId}/balance")
    @Operation(summary = "Employee Leave Balance",
            description = "HR only. Specific employee leave balance.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "userId", in = ParameterIn.PATH,
            example = "765a2d44-6423-47d9-a542-69e58d028a4d")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    public ResponseEntity<LeaveBalanceResponse> employeeBalance(
            @PathVariable String userId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(leaveService.employeeBalance(userId, year));
    }

    @GetMapping("/report")
    @Operation(summary = "Leave Report",
            description = "HR only. Monthly leave summary.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    public ResponseEntity<List<LeaveReportRowResponse>> report(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(leaveService.report(month, year));
    }

    @PutMapping("/employee/{userId}/salary")
    @Operation(summary = "Update Employee Salary",
            description = "HR only. Update employee monthly salary. Per day = salary / 26 working days.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "userId", in = ParameterIn.PATH,
            example = "765a2d44-6423-47d9-a542-69e58d028a4d")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "salary": 45000.0
            }
            """)))
    @ApiResponse(responseCode = "200", description = "Salary updated",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Salary updated successfully"
            }
            """)))
    public ResponseEntity<MessageResponse> updateSalary(
            @PathVariable String userId,
            @Valid @RequestBody UpdateSalaryRequest request) {
        leaveService.updateSalary(userId, request.salary());
        return ResponseEntity.ok(new MessageResponse(true, "Salary updated successfully"));
    }
}
