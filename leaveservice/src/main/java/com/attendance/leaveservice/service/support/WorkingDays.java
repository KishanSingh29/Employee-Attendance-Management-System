package com.attendance.leaveservice.service.support;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Weekday (Mon–Fri) counting helpers. Saturdays and Sundays are never counted
 * towards a leave, per the business rules.
 */
public final class WorkingDays {

    private WorkingDays() {
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** Weekdays in {@code [start, end]} inclusive. Returns 0 if the range is inverted. */
    public static int countBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!isWeekend(d)) {
                count++;
            }
        }
        return count;
    }

    /** Weekdays of {@code [reqStart, reqEnd]} that also fall inside {@code [windowStart, windowEnd]}. */
    public static int countWithin(LocalDate reqStart, LocalDate reqEnd,
                                  LocalDate windowStart, LocalDate windowEnd) {
        LocalDate from = reqStart.isBefore(windowStart) ? windowStart : reqStart;
        LocalDate to = reqEnd.isAfter(windowEnd) ? windowEnd : reqEnd;
        return countBetween(from, to);
    }
}
