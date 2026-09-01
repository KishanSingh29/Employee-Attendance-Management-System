package com.attendance.attendanceservice.repository;

import com.attendance.attendanceservice.entity.Attendance;
import com.attendance.attendanceservice.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndDate(String userId, LocalDate date);

    List<Attendance> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate start, LocalDate end);

    List<Attendance> findByDateOrderByCheckInAsc(LocalDate date);

    List<Attendance> findByDateBetween(LocalDate start, LocalDate end);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    long countByDateAndCheckInIsNotNull(LocalDate date);
}
