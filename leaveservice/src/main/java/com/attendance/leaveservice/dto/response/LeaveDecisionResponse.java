package com.attendance.leaveservice.dto.response;

import com.attendance.leaveservice.entity.LeaveStatus;

public record LeaveDecisionResponse(
        boolean success,
        String message,
        Long leaveId,
        LeaveStatus status
) {
}
