package com.parking.service;

import com.parking.dto.AuthDtos.UserResponse;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.StaffAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StaffPermissions {
    private final StaffAccessRepository staffAccess;

    public UsernamePasswordAuthenticationToken authenticate(User user, Object credentials, Role baseRole) {
        Role role = effectiveRole(user.getId(), baseRole);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        if (baseRole == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }
        if (baseRole == Role.TICKET_COUNTER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_LEGACY_COUNTER"));
        }
        return new UsernamePasswordAuthenticationToken(user.getId(), credentials, authorities);
    }

    public UserResponse response(User user, Authentication authentication) {
        Role role = List.of(Role.ADMIN, Role.TICKET_COUNTER, Role.EMPLOYEE, Role.USER).stream()
                .filter(candidate -> hasRole(authentication, candidate.name()))
                .findFirst()
                .orElse(user.getRole());
        return UserResponse.from(user, role, hasRole(authentication, "PLATFORM_ADMIN"));
    }

    public static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN || staffAccess.existsByUser_IdAndGrantedRole(user.getId(), Role.ADMIN);
    }

    private Role effectiveRole(int userId, Role baseRole) {
        if (baseRole == Role.ADMIN || staffAccess.existsByUser_IdAndGrantedRole(userId, Role.ADMIN)) {
            return Role.ADMIN;
        }
        if (baseRole == Role.TICKET_COUNTER || staffAccess.existsByUser_IdAndGrantedRole(userId, Role.TICKET_COUNTER)) {
            return Role.TICKET_COUNTER;
        }
        return baseRole;
    }
}
