package com.attendance.leaveservice.dto.response;

import lombok.Builder;

@Builder
public record LeaveDeductionResponse(
        int unpaidDays,
        double deductionPerDay,
        double totalDeduction,
        int month,
        int year,
        Double monthlySalary,
        Double perDaySalary
) {
}
