package com.attendance.leaveservice.dto.response;

/**
 * One employee's leave activity for a month (requests whose start date falls in
 * that month), used by the HR report.
 */
public record LeaveReportRowResponse(
        String userId,
        String employeeId,
        String firstName,
        String lastName,
        int totalRequests,
        int pendingRequests,
        int approvedRequests,
        int rejectedRequests,
        int paidDaysApproved,
        int sickDaysApproved,
        int unpaidDaysApproved,
        int totalDaysApproved
) {
}
