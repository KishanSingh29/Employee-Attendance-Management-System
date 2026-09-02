package com.attendance.leaveservice.dto.response;

import com.attendance.leaveservice.entity.LeaveBalance;

public record LeaveBalanceResponse(
        String userId,
        int year,
        int plTotal,
        int plUsed,
        int plRemaining,
        int slTotal,
        int slUsed,
        int slRemaining,
        int unpaidUsed
) {

    public static LeaveBalanceResponse from(LeaveBalance b) {
        return new LeaveBalanceResponse(
                b.getUserId(),
                b.getYear(),
                b.getPlTotal(),
                b.getPlUsed(),
                b.plRemaining(),
                b.getSlTotal(),
                b.getSlUsed(),
                b.slRemaining(),
                b.getUnpaidUsed());
    }
}
