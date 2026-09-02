package com.attendance.leaveservice.service;

import com.attendance.leaveservice.dto.response.LeaveBalanceResponse;
import com.attendance.leaveservice.entity.LeaveBalance;
import com.attendance.leaveservice.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepository;
    private final int paidTotal;
    private final int sickTotal;

    public LeaveBalanceServiceImpl(LeaveBalanceRepository balanceRepository,
                                   @Value("${leave.paid-total:12}") int paidTotal,
                                   @Value("${leave.sick-total:6}") int sickTotal) {
        this.balanceRepository = balanceRepository;
        this.paidTotal = paidTotal;
        this.sickTotal = sickTotal;
    }

    @Override
    @Transactional
    public LeaveBalance getOrCreate(String userId, int year) {
        return balanceRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> balanceRepository.save(LeaveBalance.builder()
                        .userId(userId)
                        .year(year)
                        .plTotal(paidTotal)
                        .plUsed(0)
                        .slTotal(sickTotal)
                        .slUsed(0)
                        .unpaidUsed(0)
                        .build()));
    }

    @Override
    @Transactional
    public LeaveBalanceResponse view(String userId, int year) {
        return LeaveBalanceResponse.from(getOrCreate(userId, year));
    }
}
