package com.attendance.leaveservice.service;

import com.attendance.leaveservice.dto.request.ApplyLeaveRequest;
import com.attendance.leaveservice.dto.response.ApplyLeaveResponse;
import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.dto.response.LeaveDecisionResponse;
import com.attendance.leaveservice.dto.response.LeaveDeductionResponse;
import com.attendance.leaveservice.dto.response.LeaveReportRowResponse;
import com.attendance.leaveservice.dto.response.LeaveRequestResponse;
import com.attendance.leaveservice.dto.response.MessageResponse;
import com.attendance.leaveservice.entity.LeaveBalance;
import com.attendance.leaveservice.entity.LeaveRequest;
import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.entity.LeaveType;
import com.attendance.leaveservice.exception.LeaveValidationException;
import com.attendance.leaveservice.exception.ResourceNotFoundException;
import com.attendance.leaveservice.exception.UnauthorizedAccessException;
import com.attendance.leaveservice.repository.LeaveBalanceRepository;
import com.attendance.leaveservice.repository.LeaveRequestRepository;
import com.attendance.leaveservice.security.AuthenticatedUser;
import com.attendance.leaveservice.service.support.WorkingDays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final List<LeaveStatus> ACTIVE_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceService leaveBalanceService;

    private final double monthlySalary;
    private final int workingDaysPerMonth;

    public LeaveServiceImpl(LeaveRequestRepository leaveRequestRepository,
                            LeaveBalanceRepository leaveBalanceRepository,
                            LeaveBalanceService leaveBalanceService,
                            @Value("${leave.monthly-salary:30000}") double monthlySalary,
                            @Value("${leave.working-days-per-month:30}") int workingDaysPerMonth) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.monthlySalary = monthlySalary;
        this.workingDaysPerMonth = workingDaysPerMonth;
    }

    // ------------------------------------------------------------------
    // Employee operations
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ApplyLeaveResponse apply(AuthenticatedUser user, ApplyLeaveRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();

        if (end.isBefore(start)) {
            throw new LeaveValidationException("endDate must not be before startDate");
        }
        if (!start.isAfter(LocalDate.now())) {
            throw new LeaveValidationException("Leave can only be applied for future dates");
        }

        int totalDays = WorkingDays.countBetween(start, end);
        if (totalDays == 0) {
            throw new LeaveValidationException("The selected range contains no working days");
        }

        List<LeaveRequest> active =
                leaveRequestRepository.findByUserIdAndStatusIn(user.userId(), ACTIVE_STATUSES);

        boolean overlaps = active.stream()
                .anyMatch(r -> !start.isAfter(r.getEndDate()) && !r.getStartDate().isAfter(end));
        if (overlaps) {
            throw new LeaveValidationException("You already have a leave request overlapping these dates");
        }

        LeaveBalance balance = leaveBalanceService.getOrCreate(user.userId(), start.getYear());
        assertBalanceAvailable(request.leaveType(), totalDays, balance, active);

        LeaveRequest saved = leaveRequestRepository.save(LeaveRequest.builder()
                .userId(user.userId())
                .employeeId(user.employeeId())
                .leaveType(request.leaveType())
                .startDate(start)
                .endDate(end)
                .totalDays(totalDays)
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build());

        return new ApplyLeaveResponse(true, "Leave applied successfully",
                saved.getId(), totalDays, saved.getLeaveType(), saved.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> myRequests(String userId) {
        return leaveRequestRepository.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(LeaveRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse myBalance(String userId, Integer year) {
        return leaveBalanceService.view(userId, resolveYear(year));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveDeductionResponse deduction(String userId, Integer month, Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = resolveYear(year);
        validateMonth(m);

        YearMonth ym = YearMonth.of(y, m);
        LocalDate windowStart = ym.atDay(1);
        LocalDate windowEnd = ym.atEndOfMonth();

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndYear(userId, y)
                .orElseGet(() -> leaveBalanceService.getOrCreate(userId, y));

        double resolvedMonthlySalary = (balance.getMonthlySalary() != null && balance.getMonthlySalary() > 0)
                ? balance.getMonthlySalary()
                : monthlySalary;

        double perDay = roundToTwo(resolvedMonthlySalary / 26.0);

        int unpaidDays = leaveRequestRepository
                .findByUserIdAndLeaveTypeAndStatus(userId, LeaveType.UNPAID, LeaveStatus.APPROVED).stream()
                .mapToInt(r -> WorkingDays.countWithin(r.getStartDate(), r.getEndDate(), windowStart, windowEnd))
                .sum();

        double total = roundToTwo(unpaidDays * perDay);

        return LeaveDeductionResponse.builder()
                .unpaidDays(unpaidDays)
                .deductionPerDay(perDay)
                .perDaySalary(perDay)
                .monthlySalary(resolvedMonthlySalary)
                .totalDeduction(total)
                .month(m)
                .year(y)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse cancel(AuthenticatedUser user, Long leaveId) {
        LeaveRequest request = findRequest(leaveId);
        if (!request.getUserId().equals(user.userId())) {
            throw new UnauthorizedAccessException("This leave request does not belong to you");
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveValidationException("Only pending requests can be cancelled");
        }
        leaveRequestRepository.delete(request);
        return new MessageResponse(true, "Leave request cancelled");
    }

    // ------------------------------------------------------------------
    // HR operations
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> pending() {
        return leaveRequestRepository.findByStatusOrderByAppliedAtAsc(LeaveStatus.PENDING).stream()
                .map(LeaveRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveDecisionResponse approve(AuthenticatedUser hr, Long leaveId) {
        LeaveRequest request = findRequest(leaveId);
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveValidationException("Only pending requests can be approved");
        }

        LeaveBalance balance = leaveBalanceService.getOrCreate(request.getUserId(), request.getStartDate().getYear());
        applyApprovedUsage(request, balance);
        leaveBalanceRepository.save(balance);

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedBy(hr.userId());
        leaveRequestRepository.save(request);

        return new LeaveDecisionResponse(true, "Leave approved", request.getId(), LeaveStatus.APPROVED);
    }

    @Override
    @Transactional
    public LeaveDecisionResponse reject(AuthenticatedUser hr, Long leaveId, String reason) {
        LeaveRequest request = findRequest(leaveId);
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveValidationException("Only pending requests can be rejected");
        }
        request.setStatus(LeaveStatus.REJECTED);
        request.setApprovedBy(hr.userId());
        request.setRemarks(reason);
        leaveRequestRepository.save(request);

        return new LeaveDecisionResponse(true, "Leave rejected", request.getId(), LeaveStatus.REJECTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> all(LeaveStatus statusFilter) {
        List<LeaveRequest> requests = statusFilter == null
                ? leaveRequestRepository.findByOrderByAppliedAtDesc()
                : leaveRequestRepository.findByStatusOrderByAppliedAtDesc(statusFilter);
        return requests.stream().map(LeaveRequestResponse::from).toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse employeeBalance(String userId, Integer year) {
        return leaveBalanceService.view(userId, resolveYear(year));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveReportRowResponse> report(int month, int year) {
        validateMonth(month);
        YearMonth ym = YearMonth.of(year, month);

        Map<String, List<LeaveRequest>> byUser = leaveRequestRepository
                .findByStartDateBetween(ym.atDay(1), ym.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(LeaveRequest::getUserId));

        return byUser.values().stream()
                .map(this::toReportRow)
                .sorted(Comparator.comparing(LeaveReportRowResponse::employeeId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    @Transactional
    public void updateSalary(String userId, Double salary) {
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndYear(userId, resolveYear(null))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        balance.setMonthlySalary(salary);
        leaveBalanceRepository.save(balance);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void assertBalanceAvailable(LeaveType type, int requestedDays,
                                        LeaveBalance balance, List<LeaveRequest> activeRequests) {
        switch (type) {
            case PAID -> {
                int available = balance.plRemaining() - pendingDays(activeRequests, LeaveType.PAID);
                if (available < requestedDays) {
                    throw new LeaveValidationException(
                            "Insufficient paid leave balance (available " + available
                                    + ", requested " + requestedDays + ")");
                }
            }
            case SICK -> {
                int available = balance.slRemaining() - pendingDays(activeRequests, LeaveType.SICK);
                if (available < requestedDays) {
                    throw new LeaveValidationException(
                            "Insufficient sick leave balance (available " + available
                                    + ", requested " + requestedDays + ")");
                }
            }
            case UNPAID -> {
                // No allowance to check – unpaid leave is always allowed.
            }
        }
    }

    private void applyApprovedUsage(LeaveRequest request, LeaveBalance balance) {
        int days = request.getTotalDays();
        switch (request.getLeaveType()) {
            case PAID -> {
                if (balance.plRemaining() < days) {
                    throw new LeaveValidationException("Employee no longer has enough paid leave balance");
                }
                balance.setPlUsed(balance.getPlUsed() + days);
            }
            case SICK -> {
                if (balance.slRemaining() < days) {
                    throw new LeaveValidationException("Employee no longer has enough sick leave balance");
                }
                balance.setSlUsed(balance.getSlUsed() + days);
            }
            case UNPAID -> balance.setUnpaidUsed(balance.getUnpaidUsed() + days);
        }
    }

    private static int pendingDays(List<LeaveRequest> requests, LeaveType type) {
        return requests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.PENDING && r.getLeaveType() == type)
                .mapToInt(LeaveRequest::getTotalDays)
                .sum();
    }

    private LeaveReportRowResponse toReportRow(List<LeaveRequest> requests) {
        LeaveRequest sample = requests.get(0);

        int pending = (int) requests.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING).count();
        int approved = (int) requests.stream().filter(r -> r.getStatus() == LeaveStatus.APPROVED).count();
        int rejected = (int) requests.stream().filter(r -> r.getStatus() == LeaveStatus.REJECTED).count();

        int paidDays = approvedDays(requests, LeaveType.PAID);
        int sickDays = approvedDays(requests, LeaveType.SICK);
        int unpaidDays = approvedDays(requests, LeaveType.UNPAID);

        return new LeaveReportRowResponse(
                sample.getUserId(),
                sample.getEmployeeId(),
                sample.getFirstName(),
                sample.getLastName(),
                requests.size(),
                pending,
                approved,
                rejected,
                paidDays,
                sickDays,
                unpaidDays,
                paidDays + sickDays + unpaidDays);
    }

    private static int approvedDays(List<LeaveRequest> requests, LeaveType type) {
        return requests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED && r.getLeaveType() == type)
                .mapToInt(LeaveRequest::getTotalDays)
                .sum();
    }

    private LeaveRequest findRequest(Long leaveId) {
        return leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("No leave request found with id " + leaveId));
    }

    private static int resolveYear(Integer year) {
        return year != null ? year : LocalDate.now().getYear();
    }

    private static void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new LeaveValidationException("month must be between 1 and 12");
        }
    }

    private static double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
