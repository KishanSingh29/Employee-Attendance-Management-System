package com.attendance.authservice.dto.response;

import com.attendance.authservice.entity.Role;
import com.attendance.authservice.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        String email,
        Role role,
        String department,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmployeeId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.getCreatedAt()
        );
    }
}
