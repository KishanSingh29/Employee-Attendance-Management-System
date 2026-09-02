package com.attendance.leaveservice.dto.request;

import com.attendance.leaveservice.entity.LeaveType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplyLeaveRequest(

        @NotNull(message = "leaveType is required")
        LeaveType leaveType,

        @NotNull(message = "startDate is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason
) {
}
