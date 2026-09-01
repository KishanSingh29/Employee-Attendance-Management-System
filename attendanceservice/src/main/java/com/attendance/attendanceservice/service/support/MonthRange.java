package com.attendance.attendanceservice.service.support;

import com.attendance.attendanceservice.exception.AttendanceException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The calendar bounds of a requested month plus a helper for counting the
 * weekdays that have already elapsed (used to estimate "absent" days, since
 * this service only stores rows for days an employee actually checked in).
 */
public record MonthRange(int month, int year, LocalDate start, LocalDate end) {

    public static MonthRange of(int month, int year) {
        if (month < 1 || month > 12) {
            throw new AttendanceException("month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new AttendanceException("year is out of range");
        }
        YearMonth ym = YearMonth.of(year, month);
        return new MonthRange(month, year, ym.atDay(1), ym.atEndOfMonth());
    }

    /** Weekdays (Mon–Fri) in this month up to and including today, capped at month end. */
    public long weekdaysElapsed(LocalDate today) {
        LocalDate limit = today.isBefore(end) ? today : end;
        if (limit.isBefore(start)) {
            return 0;
        }
        long count = 0;
        for (LocalDate d = start; !d.isAfter(limit); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }
}
