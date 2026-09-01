package com.attendance.attendanceservice.service;

import com.attendance.attendanceservice.dto.request.ProfileSyncRequest;
import com.attendance.attendanceservice.dto.response.EmployeeProfileResponse;
import com.attendance.attendanceservice.entity.EmployeeProfile;
import com.attendance.attendanceservice.security.AuthenticatedUser;

public interface EmployeeProfileService {

    /** Create or update the local identity copy for an employee (HR / sync job). */
    EmployeeProfileResponse sync(ProfileSyncRequest request);

    EmployeeProfileResponse getByUserId(String userId);

    /**
     * Guarantee a profile row exists for the caller so attendance always has an
     * owner. Missing fields (name, department) are filled by a later {@link #sync}.
     */
    EmployeeProfile ensureProfile(AuthenticatedUser user);
}
