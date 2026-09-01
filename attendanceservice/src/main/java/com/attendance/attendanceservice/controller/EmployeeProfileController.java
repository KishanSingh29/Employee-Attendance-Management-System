package com.attendance.attendanceservice.controller;

import com.attendance.attendanceservice.dto.request.ProfileSyncRequest;
import com.attendance.attendanceservice.dto.response.EmployeeProfileResponse;
import com.attendance.attendanceservice.security.AuthenticatedUser;
import com.attendance.attendanceservice.service.EmployeeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendance/profiles")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employee Profiles", description = "Local mirror of employee identity data owned by authservice")
public class EmployeeProfileController {

    private final EmployeeProfileService profileService;

    public EmployeeProfileController(EmployeeProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Create or update an employee profile (HR only)",
            description = "Called by HR or an authservice sync job to keep names / department in sync.")
    public ResponseEntity<EmployeeProfileResponse> sync(@Valid @RequestBody ProfileSyncRequest request) {
        return ResponseEntity.ok(profileService.sync(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @Operation(summary = "Get the caller's profile as stored in this service")
    public ResponseEntity<EmployeeProfileResponse> myProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(profileService.getByUserId(user.userId()));
    }
}
