package com.attendance.authservice.repository;

import com.attendance.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    boolean existsByEmail(String email);

    /**
     * Highest employeeId currently stored, e.g. {@code EMP014}. Used to derive
     * the next sequential id.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT u.employeeId FROM User u ORDER BY u.employeeId DESC LIMIT 1")
    Optional<String> findTopEmployeeId();
}
