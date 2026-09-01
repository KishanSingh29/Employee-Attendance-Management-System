package com.attendance.attendanceservice.event;

import com.attendance.attendanceservice.entity.EmployeeProfile;
import com.attendance.attendanceservice.repository.EmployeeProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps a local {@link EmployeeProfile} whenever a new user is registered in
 * authservice. Idempotent: an existing profile (or a concurrent insert) is
 * skipped, not overwritten.
 */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final EmployeeProfileRepository profileRepository;

    public UserRegisteredConsumer(EmployeeProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.user-registered:user-registered}",
            groupId = "${spring.kafka.consumer.group-id:attendance-group}")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null || event.userId() == null) {
            log.warn("Ignoring user-registered event with no userId: {}", event);
            return;
        }

        try {
            if (profileRepository.existsByUserId(event.userId())) {
                log.info("EmployeeProfile already exists for userId={}, skipping", event.userId());
                return;
            }

            profileRepository.save(EmployeeProfile.builder()
                    .userId(event.userId())
                    .employeeId(event.employeeId())
                    .firstName(event.firstName())
                    .lastName(event.lastName())
                    .email(event.email())
                    .department(event.department())
                    .build());

            log.info("Created EmployeeProfile for userId={} employeeId={}", event.userId(), event.employeeId());
        } catch (DataIntegrityViolationException ex) {
            log.info("EmployeeProfile for userId={} was created concurrently, skipping", event.userId());
        } catch (Exception ex) {
            log.error("Failed to process user-registered event for userId={}", event.userId(), ex);
        }
    }
}
