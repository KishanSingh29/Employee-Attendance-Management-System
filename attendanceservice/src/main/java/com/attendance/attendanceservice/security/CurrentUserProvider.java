package com.attendance.attendanceservice.security;

import com.attendance.attendanceservice.exception.UnauthorizedAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Convenience access to the authenticated caller plus the guard rule that an
 * {@code EMPLOYEE} may only touch their own records.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedAccessException("No authenticated user in the security context");
        }
        return user;
    }

    /**
     * Resolves the user id whose data is being requested. The JWT subject is the
     * source of truth; the optional {@code X-User-Id} header must match it for a
     * non-HR caller.
     */
    public String resolveTargetUserId(String headerUserId) {
        AuthenticatedUser current = require();
        if (StringUtils.hasText(headerUserId)
                && !current.isHr()
                && !headerUserId.equals(current.userId())) {
            throw new UnauthorizedAccessException("X-User-Id does not match the authenticated user");
        }
        return current.userId();
    }
}
