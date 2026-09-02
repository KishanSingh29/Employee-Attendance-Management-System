package com.attendance.leaveservice.dto.response;

import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.entity.LeaveType;

public record ApplyLeaveResponse(
        boolean success,
        String message,
        Long leaveId,
        int totalDays,
        LeaveType leaveType,
        LeaveStatus status
) {
}
