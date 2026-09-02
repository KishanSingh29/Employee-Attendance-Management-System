package com.attendance.leaveservice.repository;

import com.attendance.leaveservice.entity.LeaveRequest;
import com.attendance.leaveservice.entity.LeaveStatus;
import com.attendance.leaveservice.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserIdOrderByAppliedAtDesc(String userId);

    List<LeaveRequest> findByStatusOrderByAppliedAtAsc(LeaveStatus status);

    List<LeaveRequest> findByOrderByAppliedAtDesc();

    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveStatus status);

    /** Requests for a user in the given states – used for overlap and pending-balance checks. */
    List<LeaveRequest> findByUserIdAndStatusIn(String userId, Collection<LeaveStatus> statuses);

    List<LeaveRequest> findByUserIdAndLeaveTypeAndStatus(String userId, LeaveType leaveType, LeaveStatus status);

    /** All requests whose start date falls inside the range – used for the monthly report. */
    List<LeaveRequest> findByStartDateBetween(LocalDate start, LocalDate end);
}
