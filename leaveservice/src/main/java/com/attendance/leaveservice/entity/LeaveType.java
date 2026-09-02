package com.attendance.leaveservice.entity;

public enum LeaveType {
    /** Paid leave, deducted from the yearly PL allowance. */
    PAID,
    /** Sick leave, deducted from the yearly SL allowance. */
    SICK,
    /** Unpaid leave, tracked only for payroll deduction. */
    UNPAID
}
