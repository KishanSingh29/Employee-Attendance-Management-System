package com.attendance.leaveservice.dto.response;

import com.attendance.leaveservice.entity.LeaveRequest;
import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.entity.LeaveType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long id,
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        LeaveType leaveType,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        int totalDays,
        String reason,
        LeaveStatus status,
        String approvedBy,
        String remarks,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt
) {

    public static LeaveRequestResponse from(LeaveRequest r) {
        return new LeaveRequestResponse(
                r.getId(),
                r.getUserId(),
                r.getEmployeeId(),
                r.getFirstName(),
                r.getLastName(),
                r.getLeaveType(),
                r.getStartDate(),
                r.getEndDate(),
                r.getTotalDays(),
                r.getReason(),
                r.getStatus(),
                r.getApprovedBy(),
                r.getRemarks(),
                r.getAppliedAt(),
                r.getUpdatedAt());
    }
}
