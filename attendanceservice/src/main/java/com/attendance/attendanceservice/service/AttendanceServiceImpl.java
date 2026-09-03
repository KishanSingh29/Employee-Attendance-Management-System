package com.attendance.attendanceservice.service;

import com.attendance.attendanceservice.dto.response.AttendanceResponse;
import com.attendance.attendanceservice.dto.response.AttendanceSummaryResponse;
import com.attendance.attendanceservice.dto.response.CheckInResponse;
import com.attendance.attendanceservice.dto.response.CheckOutResponse;
import com.attendance.attendanceservice.dto.response.EmployeeMonthlyReportResponse;
import com.attendance.attendanceservice.dto.response.HrAttendanceRecordResponse;
import com.attendance.attendanceservice.dto.response.HrDashboardResponse;
import com.attendance.attendanceservice.dto.response.TodayAttendanceResponse;
import com.attendance.attendanceservice.entity.Attendance;
import com.attendance.attendanceservice.entity.AttendanceStatus;
import com.attendance.attendanceservice.entity.EmployeeProfile;
import com.attendance.attendanceservice.exception.AttendanceException;
import com.attendance.attendanceservice.exception.ResourceNotFoundException;
import com.attendance.attendanceservice.repository.AttendanceRepository;
import com.attendance.attendanceservice.repository.EmployeeProfileRepository;
import com.attendance.attendanceservice.security.AuthenticatedUser;
import com.attendance.attendanceservice.service.support.MonthRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeProfileRepository profileRepository;
    private final EmployeeProfileService profileService;

    private final LocalTime lateThreshold;
    private final int halfDayHours;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository,
                                 EmployeeProfileRepository profileRepository,
                                 EmployeeProfileService profileService,
                                 @Value("${attendance.late-threshold:09:30:00}") String lateThreshold,
                                 @Value("${attendance.half-day-hours:4}") int halfDayHours) {
        this.attendanceRepository = attendanceRepository;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.lateThreshold = LocalTime.parse(lateThreshold);
        this.halfDayHours = halfDayHours;
    }

    // ------------------------------------------------------------------
    // Employee operations
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public CheckInResponse checkIn(AuthenticatedUser user) {
        profileService.ensureProfile(user);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withNano(0);

        // If the employee forgot to check out yesterday, close that record at 23:59
        // before starting today's, so a missed check-out never blocks a new day.
        autoCloseMissedCheckOut(user.userId(), today);

        Attendance attendance = attendanceRepository.findByUserIdAndDate(user.userId(), today)
                .orElseGet(() -> Attendance.builder()
                        .userId(user.userId())
                        .date(today)
                        .status(AttendanceStatus.ABSENT)
                        .build());

        if (attendance.getCheckIn() != null) {
            throw new AttendanceException("You have already checked in today at " + attendance.getCheckIn());
        }

        AttendanceStatus status = now.isAfter(lateThreshold) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
        attendance.setCheckIn(now);
        attendance.setStatus(status);
        attendanceRepository.save(attendance);

        return new CheckInResponse(true, "Checked in successfully", now, status);
    }

    @Override
    @Transactional
    public CheckOutResponse checkOut(AuthenticatedUser user) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withNano(0);

        Attendance attendance = attendanceRepository.findByUserIdAndDate(user.userId(), today)
                .filter(a -> a.getCheckIn() != null)
                .orElseThrow(() -> new AttendanceException("You cannot check out before checking in"));

        if (attendance.getCheckOut() != null) {
            throw new AttendanceException("You have already checked out today at " + attendance.getCheckOut());
        }

        // Span the real dates so an overnight shift (check-in and check-out on
        // different calendar days) does not produce zero or negative hours.
        LocalDateTime checkInDateTime = LocalDateTime.of(attendance.getDate(), attendance.getCheckIn());
        LocalDateTime checkOutDateTime = LocalDateTime.of(LocalDate.now(), now);
        if (checkOutDateTime.isBefore(checkInDateTime)) {
            throw new AttendanceException("Check-out time cannot be earlier than check-in time");
        }

        Duration duration = Duration.between(checkInDateTime, checkOutDateTime);
        double workingHours = roundToTwo(duration.toMinutes() / 60.0);

        AttendanceStatus status;
        if (workingHours < halfDayHours) {
            status = AttendanceStatus.HALF_DAY;
        } else if (attendance.getStatus() == AttendanceStatus.LATE) {
            status = AttendanceStatus.LATE;
        } else {
            status = AttendanceStatus.PRESENT;
        }

        attendance.setCheckOut(now);
        attendance.setWorkingHours(workingHours);
        attendance.setStatus(status);
        attendanceRepository.save(attendance);

        return new CheckOutResponse(true, "Checked out successfully", now, workingHours, status);
    }

    @Override
    @Transactional(readOnly = true)
    public TodayAttendanceResponse getToday(String userId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByUserIdAndDate(userId, today)
                .map(TodayAttendanceResponse::from)
                .orElseGet(() -> TodayAttendanceResponse.notCheckedIn(today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getHistory(String userId, int month, int year) {
        MonthRange range = MonthRange.of(month, year);
        return attendanceRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, range.start(), range.end())
                .stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(String userId, int month, int year) {
        MonthRange range = MonthRange.of(month, year);
        List<Attendance> records = attendanceRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, range.start(), range.end());
        return aggregate(records, range).toSummary(month, year);
    }

    // ------------------------------------------------------------------
    // HR operations
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<HrAttendanceRecordResponse> getAllForDate(LocalDate date) {
        List<Attendance> records = attendanceRepository.findByDateOrderByCheckInAsc(date);
        Map<String, EmployeeProfile> profiles = profilesByUserId(
                records.stream().map(Attendance::getUserId).distinct().toList());

        return records.stream()
                .map(a -> HrAttendanceRecordResponse.of(a, profiles.get(a.getUserId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getEmployeeHistory(String userId, int month, int year) {
        if (!profileRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("No employee found with id " + userId);
        }
        return getHistory(userId, month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public HrDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        long totalEmployees = profileRepository.count();
        long presentToday = attendanceRepository.countByDateAndCheckInIsNotNull(today);
        long lateToday = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LATE);
        long halfDayToday = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.HALF_DAY);
        long onLeaveToday = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.ON_LEAVE);
        long absentToday = Math.max(0, totalEmployees - presentToday - onLeaveToday);

        return new HrDashboardResponse(totalEmployees, presentToday, absentToday, onLeaveToday, lateToday, halfDayToday);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeMonthlyReportResponse> getMonthlyReport(int month, int year) {
        MonthRange range = MonthRange.of(month, year);
        Map<String, List<Attendance>> byUser = attendanceRepository
                .findByDateBetween(range.start(), range.end())
                .stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        return profileRepository.findAll().stream()
                .sorted(Comparator.comparing(EmployeeProfile::getEmployeeId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(profile -> {
                    Aggregates agg = aggregate(
                            byUser.getOrDefault(profile.getUserId(), List.of()), range);
                    return agg.toReportRow(profile);
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Map<String, EmployeeProfile> profilesByUserId(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(EmployeeProfile::getUserId, Function.identity()));
    }

    private Aggregates aggregate(List<Attendance> records, MonthRange range) {
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long halfDay = records.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
        long onLeave = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();

        double totalHours = roundToTwo(records.stream()
                .map(Attendance::getWorkingHours)
                .filter(h -> h != null)
                .mapToDouble(Double::doubleValue)
                .sum());

        long daysWorked = records.stream()
                .filter(a -> a.getWorkingHours() != null && a.getWorkingHours() > 0)
                .count();
        double averageHours = daysWorked == 0 ? 0.0 : roundToTwo(totalHours / daysWorked);

        long accountedDays = present + late + halfDay + onLeave;
        long absent = Math.max(0, range.weekdaysElapsed(LocalDate.now()) - accountedDays);

        return new Aggregates(present, absent, late, halfDay, onLeave, totalHours, averageHours);
    }

    private static double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * If yesterday's record was left open (checked in, never checked out), auto-close it
     * at 23:59 of that day: compute working hours and derive the status the same way a
     * normal check-out would ({@code < halfDayHours} → HALF_DAY, otherwise keep LATE or PRESENT).
     */
    private void autoCloseMissedCheckOut(String userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        attendanceRepository.findByUserIdAndDate(userId, yesterday).ifPresent(prev -> {
            if (prev.getCheckIn() == null || prev.getCheckOut() != null) {
                return;
            }
            LocalTime autoCheckOut = LocalTime.of(23, 59);
            Duration worked = Duration.between(
                    LocalDateTime.of(prev.getDate(), prev.getCheckIn()),
                    LocalDateTime.of(prev.getDate(), autoCheckOut));
            double workingHours = roundToTwo(Math.max(0, worked.toMinutes()) / 60.0);

            AttendanceStatus status;
            if (workingHours < halfDayHours) {
                status = AttendanceStatus.HALF_DAY;
            } else if (prev.getStatus() == AttendanceStatus.LATE) {
                status = AttendanceStatus.LATE;
            } else {
                status = AttendanceStatus.PRESENT;
            }

            prev.setCheckOut(autoCheckOut);
            prev.setWorkingHours(workingHours);
            prev.setStatus(status);
            attendanceRepository.save(prev);
        });
    }

    private record Aggregates(long present, long absent, long late, long halfDay, long onLeave,
                              double totalHours, double averageHours) {

        AttendanceSummaryResponse toSummary(int month, int year) {
            return new AttendanceSummaryResponse(month, year, present, absent, late, halfDay, onLeave,
                    totalHours, averageHours);
        }

        EmployeeMonthlyReportResponse toReportRow(EmployeeProfile p) {
            return new EmployeeMonthlyReportResponse(
                    p.getUserId(), p.getEmployeeId(), p.getFirstName(), p.getLastName(), p.getDepartment(),
                    present, absent, late, halfDay, onLeave, totalHours, averageHours);
        }
    }
}
