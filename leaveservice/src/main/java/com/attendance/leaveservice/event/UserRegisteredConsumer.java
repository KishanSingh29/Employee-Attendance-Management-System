package com.attendance.leaveservice.event;

import com.attendance.leaveservice.entity.LeaveBalance;
import com.attendance.leaveservice.repository.LeaveBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Seeds a current-year {@link LeaveBalance} (PL 12 / SL 6) whenever a new user is
 * registered in authservice. Idempotent: an existing balance for the year (or a
 * concurrent insert) is skipped, not overwritten.
 */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final int paidTotal;
    private final int sickTotal;

    public UserRegisteredConsumer(LeaveBalanceRepository leaveBalanceRepository,
                                  @Value("${leave.paid-total:12}") int paidTotal,
                                  @Value("${leave.sick-total:6}") int sickTotal) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.paidTotal = paidTotal;
        this.sickTotal = sickTotal;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.user-registered:user-registered}",
            groupId = "${spring.kafka.consumer.group-id:leave-group}")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null || event.userId() == null) {
            log.warn("Ignoring user-registered event with no userId: {}", event);
            return;
        }

        int year = Year.now().getValue();

        try {
            if (leaveBalanceRepository.findByUserIdAndYear(event.userId(), year).isPresent()) {
                log.info("LeaveBalance already exists for userId={} year={}, skipping", event.userId(), year);
                return;
            }

            double salary = event.salary() != null ? event.salary() : 30000.0;

            leaveBalanceRepository.save(LeaveBalance.builder()
                    .userId(event.userId())
                    .year(year)
                    .plTotal(paidTotal)
                    .plUsed(0)
                    .slTotal(sickTotal)
                    .slUsed(0)
                    .unpaidUsed(0)
                    .monthlySalary(salary)
                    .build());

            log.info("Created LeaveBalance for userId={} year={} (PL {} / SL {}, salary {})",
                    event.userId(), year, paidTotal, sickTotal, salary);
        } catch (DataIntegrityViolationException ex) {
            log.info("LeaveBalance for userId={} year={} was created concurrently, skipping", event.userId(), year);
        } catch (Exception ex) {
            log.error("Failed to process user-registered event for userId={}", event.userId(), ex);
        }
    }
}
