package com.attendance.leaveservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSalaryRequest(

        @NotNull(message = "salary is required")
        @Min(value = 1000, message = "salary must be at least 1000")
        Double salary
) {
}
