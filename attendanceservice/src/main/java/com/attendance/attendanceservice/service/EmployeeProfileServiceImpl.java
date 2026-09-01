package com.attendance.attendanceservice.service;

import com.attendance.attendanceservice.dto.request.ProfileSyncRequest;
import com.attendance.attendanceservice.dto.response.EmployeeProfileResponse;
import com.attendance.attendanceservice.entity.EmployeeProfile;
import com.attendance.attendanceservice.exception.ResourceNotFoundException;
import com.attendance.attendanceservice.repository.EmployeeProfileRepository;
import com.attendance.attendanceservice.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeProfileServiceImpl implements EmployeeProfileService {

    private final EmployeeProfileRepository profileRepository;

    public EmployeeProfileServiceImpl(EmployeeProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public EmployeeProfileResponse sync(ProfileSyncRequest request) {
        EmployeeProfile profile = profileRepository.findByUserId(request.userId())
                .orElseGet(EmployeeProfile::new);

        profile.setUserId(request.userId());
        profile.setEmployeeId(request.employeeId());
        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setEmail(request.email().toLowerCase().trim());
        profile.setDepartment(request.department().trim());

        return EmployeeProfileResponse.from(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileResponse getByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .map(EmployeeProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No profile found for user " + userId));
    }

    @Override
    @Transactional
    public EmployeeProfile ensureProfile(AuthenticatedUser user) {
        return profileRepository.findByUserId(user.userId())
                .orElseGet(() -> profileRepository.save(EmployeeProfile.builder()
                        .userId(user.userId())
                        .employeeId(user.employeeId())
                        .email(user.email())
                        .build()));
    }
}
