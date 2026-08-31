package com.attendance.authservice.entity;

/**
 * Roles supported by the system. Spring Security authorities are derived as
 * {@code ROLE_<name>} (e.g. {@code ROLE_HR}).
 */
public enum Role {
    EMPLOYEE,
    HR
}
