package com.attendance.leaveservice.service;

import com.attendance.leaveservice.dto.request.ApplyLeaveRequest;
import com.attendance.leaveservice.dto.response.ApplyLeaveResponse;
import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.dto.response.LeaveDecisionResponse;
import com.attendance.leaveservice.dto.response.LeaveDeductionResponse;
import com.attendance.leaveservice.dto.response.LeaveReportRowResponse;
import com.attendance.leaveservice.dto.response.LeaveRequestResponse;
import com.attendance.leaveservice.dto.response.MessageResponse;
import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.security.AuthenticatedUser;

import java.util.List;

public interface LeaveService {

    // ----- employee -----
    ApplyLeaveResponse apply(AuthenticatedUser user, ApplyLeaveRequest request);

    List<LeaveRequestResponse> myRequests(String userId);

    LeaveBalanceResponse myBalance(String userId, Integer year);

    LeaveDeductionResponse deduction(String userId, Integer month, Integer year);

    MessageResponse cancel(AuthenticatedUser user, Long leaveId);

    // ----- HR -----
    List<LeaveRequestResponse> pending();

    LeaveDecisionResponse approve(AuthenticatedUser hr, Long leaveId);

    LeaveDecisionResponse reject(AuthenticatedUser hr, Long leaveId, String reason);

    List<LeaveRequestResponse> all(LeaveStatus statusFilter);

    LeaveBalanceResponse employeeBalance(String userId, Integer year);

    List<LeaveReportRowResponse> report(int month, int year);

    void updateSalary(String userId, Double salary);
}
