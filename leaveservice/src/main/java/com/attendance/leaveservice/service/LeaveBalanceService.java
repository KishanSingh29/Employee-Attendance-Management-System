package com.attendance.leaveservice.service;

import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.entity.LeaveBalance;

public interface LeaveBalanceService {

    /** Fetch the caller's balance for the year, creating a fresh allowance row if none exists. */
    LeaveBalance getOrCreate(String userId, int year);

    LeaveBalanceResponse view(String userId, int year);
}
