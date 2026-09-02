package com.attendance.authservice.service;

import com.attendance.authservice.dto.request.LoginRequest;
import com.attendance.authservice.dto.request.RefreshTokenRequest;
import com.attendance.authservice.dto.request.RegisterRequest;
import com.attendance.authservice.dto.response.LoginResponse;
import com.attendance.authservice.dto.response.MessageResponse;
import com.attendance.authservice.dto.response.RegisterResponse;
import com.attendance.authservice.dto.response.TokenRefreshResponse;
import com.attendance.authservice.dto.response.UserResponse;
import com.attendance.authservice.entity.BlacklistedToken;
import com.attendance.authservice.entity.RefreshToken;
import com.attendance.authservice.entity.User;
import com.attendance.authservice.event.UserEventProducer;
import com.attendance.authservice.event.UserRegisteredEvent;
import com.attendance.authservice.exception.EmailAlreadyExistsException;
import com.attendance.authservice.exception.InvalidTokenException;
import com.attendance.authservice.exception.ResourceNotFoundException;
import com.attendance.authservice.repository.BlacklistedTokenRepository;
import com.attendance.authservice.repository.UserRepository;
import com.attendance.authservice.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmployeeIdGenerator employeeIdGenerator;
    private final UserEventProducer userEventProducer;

    public AuthServiceImpl(UserRepository userRepository,
                           BlacklistedTokenRepository blacklistedTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           EmployeeIdGenerator employeeIdGenerator,
                           UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.employeeIdGenerator = employeeIdGenerator;
        this.userEventProducer = userEventProducer;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .employeeId(employeeIdGenerator.nextEmployeeId())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .department(request.department().trim())
                .salary(request.salary())
                .build();

        User saved = userRepository.save(user);

        userEventProducer.publishUserRegistered(UserRegisteredEvent.from(saved));

        return new RegisterResponse(true, "Employee registered successfully",
                saved.getEmployeeId(), saved.getUserId());
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.issue(user);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                user.getUserId(),
                user.getRole().name(),
                user.getEmployeeId(),
                user.getFirstName());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(refreshToken.getUser());
        return new TokenRefreshResponse(accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllEmployees() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Authorization header with a bearer token is required");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!jwtService.isTokenValid(token)) {
            throw new InvalidTokenException("Access token is invalid or already expired");
        }

        String jti = jwtService.extractJti(token);
        if (!blacklistedTokenRepository.existsByJti(jti)) {
            Date expiration = jwtService.extractExpiration(token);
            blacklistedTokenRepository.save(BlacklistedToken.builder()
                    .jti(jti)
                    .expiresAt(expiration != null ? expiration.toInstant() : Instant.now())
                    .build());
        }

        userRepository.findByUserId(jwtService.extractUserId(token))
                .ifPresent(refreshTokenService::revokeAllForUser);

        return new MessageResponse(true, "Logged out successfully");
    }
}
