package com.attendance.authservice.controller;

import com.attendance.authservice.dto.request.LoginRequest;
import com.attendance.authservice.dto.request.RefreshTokenRequest;
import com.attendance.authservice.dto.request.RegisterRequest;
import com.attendance.authservice.dto.response.LoginResponse;
import com.attendance.authservice.dto.response.MessageResponse;
import com.attendance.authservice.dto.response.RegisterResponse;
import com.attendance.authservice.dto.response.TokenRefreshResponse;
import com.attendance.authservice.dto.response.UserResponse;
import com.attendance.authservice.security.UserPrincipal;
import com.attendance.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, login, token management and user lookup")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Tag(name = "Authentication")
    @Operation(summary = "Register New Employee",
            description = "Register employee. On success, Kafka event 'user-registered' publish hota hai. AttendanceService EmployeeProfile auto-create karta hai, LeaveService LeaveBalance auto-create karta hai (PL:12, SL:6).")
    @ApiResponse(responseCode = "200", description = "Registration successful",
            content = @Content(schema = @Schema(example = """
            {
              "success": true,
              "message": "Employee registered successfully",
              "employeeId": "EMP001",
              "userId": "765a2d44-6423-47d9-a542-69e58d028a4d"
            }
            """)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "firstName": "Kishan",
              "lastName": "Singh",
              "email": "kishan@gmail.com",
              "password": "password123",
              "role": "EMPLOYEE",
              "department": "IT"
            }
            """)))
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Employee Login",
            description = "Authenticate employee. Access token expires in 1 hour, refresh token in 24 hours.")
    @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(example = """
            {
              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
              "refreshToken": "uuid",
              "userId": "765a2d44-6423-47d9-a542-69e58d028a4d",
              "role": "EMPLOYEE",
              "employeeId": "EMP001",
              "firstName": "Kishan"
            }
            """)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "email": "kishan@gmail.com",
              "password": "password123"
            }
            """)))
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token",
            description = "Get new access token using refresh token.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
            {
              "refreshToken": "89302150-187e-4d1b-82da-dd2677b3eb5d"
            }
            """)))
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Get My Profile",
            description = "Get logged in employee details.")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUsername()));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('HR')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Get All Employees",
            description = "HR only. List of all registered employees.")
    public ResponseEntity<List<UserResponse>> employees() {
        return ResponseEntity.ok(authService.getAllEmployees());
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(summary = "Logout",
            description = "Blacklists current JWT token. Token unusable after logout.")
    public ResponseEntity<MessageResponse> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(authService.logout(authorizationHeader));
    }
}

//for rest api
