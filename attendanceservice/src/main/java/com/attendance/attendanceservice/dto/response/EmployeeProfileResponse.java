package com.attendance.attendanceservice.dto.response;

import com.attendance.attendanceservice.entity.EmployeeProfile;

import java.time.LocalDateTime;

public record EmployeeProfileResponse(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        String email,
        String department,
        LocalDateTime createdAt
) {

    public static EmployeeProfileResponse from(EmployeeProfile p) {
        return new EmployeeProfileResponse(
                p.getUserId(),
                p.getEmployeeId(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getDepartment(),
                p.getCreatedAt());
    }
}
