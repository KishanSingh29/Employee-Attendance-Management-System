package com.attendance.authservice.service;

import com.attendance.authservice.dto.request.LoginRequest;
import com.attendance.authservice.dto.request.RefreshTokenRequest;
import com.attendance.authservice.dto.request.RegisterRequest;
import com.attendance.authservice.dto.response.LoginResponse;
import com.attendance.authservice.dto.response.MessageResponse;
import com.attendance.authservice.dto.response.RegisterResponse;
import com.attendance.authservice.dto.response.TokenRefreshResponse;
import com.attendance.authservice.dto.response.UserResponse;

import java.util.List;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    TokenRefreshResponse refresh(RefreshTokenRequest request);

    UserResponse getCurrentUser(String userId);

    List<UserResponse> getAllEmployees();

    MessageResponse logout(String authorizationHeader);
}
