package com.attendance.attendanceservice.repository;

import com.attendance.attendanceservice.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    Optional<EmployeeProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<EmployeeProfile> findByUserIdIn(Collection<String> userIds);
}
