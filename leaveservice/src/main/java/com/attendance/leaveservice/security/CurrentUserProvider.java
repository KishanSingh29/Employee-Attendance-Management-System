package com.attendance.leaveservice.security;

import com.attendance.leaveservice.exception.UnauthorizedAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience access to the authenticated caller.
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
}
