package com.attendance.attendanceservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload used by HR (or an authservice sync job) to mirror an employee's
 * identity into this service.
 */
public record ProfileSyncRequest(

        @NotBlank(message = "userId is required")
        @Size(max = 36)
        String userId,

        @NotBlank(message = "employeeId is required")
        @Size(max = 20)
        String employeeId,

        @NotBlank(message = "firstName is required")
        @Size(max = 50)
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 50)
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "department is required")
        @Size(max = 60)
        String department
) {
}
