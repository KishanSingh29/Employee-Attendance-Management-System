package com.attendance.leaveservice.controller;

import com.attendance.leaveservice.dto.request.ApplyLeaveRequest;
import com.attendance.leaveservice.dto.response.ApplyLeaveResponse;
import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.dto.response.LeaveDeductionResponse;
import com.attendance.leaveservice.dto.response.LeaveRequestResponse;
import com.attendance.leaveservice.dto.response.MessageResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leave")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Leave", description = "Apply for leave and view the caller's own requests, balance and deductions")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    @Tag(name = "Leave")
    @Operation(summary = "Apply for Leave",
            description = "Apply PAID/SICK/UNPAID leave. Weekend days excluded. Future dates only. Balance auto-checked.")
    @SecurityRequirement(name = "Bearer Auth")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "leaveType": "PAID",
              "startDate": "2026-09-05",
              "endDate": "2026-09-07",
              "reason": "Personal work"
            }
            """)))
    @ApiResponse(responseCode = "201", description = "Leave applied",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Leave applied successfully",
              "leaveId": 1,
              "totalDays": 1,
              "leaveType": "PAID",
              "status": "PENDING"
            }
            """)))
    public ResponseEntity<ApplyLeaveResponse> apply(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ApplyLeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(user, request));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "My Leave Requests",
            description = "All leave requests with status PENDING/APPROVED/REJECTED.")
    @SecurityRequirement(name = "Bearer Auth")
    public ResponseEntity<List<LeaveRequestResponse>> myRequests(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(leaveService.myRequests(user.userId()));
    }

    @GetMapping("/balance")
    @Operation(summary = "Leave Balance",
            description = "Remaining PL and SL balance for the year.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    @ApiResponse(responseCode = "200", description = "Leave balance",
            content = @Content(schema = @Schema(example = """
            {
              "userId": "uuid",
              "year": 2026,
              "plTotal": 12,
              "plUsed": 1,
              "plRemaining": 11,
              "slTotal": 6,
              "slUsed": 0,
              "slRemaining": 6,
              "unpaidUsed": 0
            }
            """)))
    public ResponseEntity<LeaveBalanceResponse> balance(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(leaveService.myBalance(user.userId(), year));
    }

    @GetMapping("/deduction")
    @Operation(summary = "Leave Deduction",
            description = "Salary deduction for unpaid leaves. Per day = monthly salary / 26 working days.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "month", in = ParameterIn.QUERY, example = "9")
    @Parameter(name = "year", in = ParameterIn.QUERY, example = "2026")
    @ApiResponse(responseCode = "200", description = "Deduction details",
            content = @Content(schema = @Schema(example = """
            {
              "unpaidDays": 2,
              "monthlySalary": 45000.0,
              "perDaySalary": 1730.77,
              "deductionPerDay": 1730.77,
              "totalDeduction": 3461.54,
              "month": 9,
              "year": 2026
            }
            """)))
    public ResponseEntity<LeaveDeductionResponse> deduction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(leaveService.deduction(user.userId(), month, year));
    }

    @PutMapping("/cancel/{leaveId}")
    @Operation(summary = "Cancel Leave",
            description = "Cancel PENDING leave request only.")
    @SecurityRequirement(name = "Bearer Auth")
    @Parameter(name = "leaveId", in = ParameterIn.PATH, example = "1")
    public ResponseEntity<MessageResponse> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.cancel(user, leaveId));
    }
}
