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
    @Operation(summary = "Register a new employee", description = "Public endpoint. Generates the next employee id and stores a BCrypt-hashed password.")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain tokens", description = "Public endpoint. Returns a 1-hour access token and a 24-hour refresh token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUsername()));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('HR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all employees (HR only)")
    public ResponseEntity<List<UserResponse>> employees() {
        return ResponseEntity.ok(authService.getAllEmployees());
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Invalidate the current access token and revoke refresh tokens")
    public ResponseEntity<MessageResponse> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(authService.logout(authorizationHeader));
    }
}

//for rest api
