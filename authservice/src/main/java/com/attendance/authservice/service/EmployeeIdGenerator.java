package com.attendance.authservice.service;

import com.attendance.authservice.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Produces sequential employee identifiers in the form {@code EMP001}, {@code EMP002}, ...
 * The unique constraint on {@code users.employee_id} is the final guard against races.
 */
@Component
public class EmployeeIdGenerator {

    private static final String PREFIX = "EMP";
    private static final int PADDING = 3;

    private final UserRepository userRepository;

    public EmployeeIdGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String nextEmployeeId() {
        long next = userRepository.findTopEmployeeId()
                .map(this::parseSequence)
                .orElse(0L) + 1;
        return PREFIX + String.format("%0" + PADDING + "d", next);
    }

    private long parseSequence(String employeeId) {
        try {
            return Long.parseLong(employeeId.substring(PREFIX.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return 0L;
        }
    }
}
