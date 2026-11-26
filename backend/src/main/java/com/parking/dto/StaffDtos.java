package com.parking.dto;

import com.parking.entity.Role;
import com.parking.entity.StaffAccess;

public final class StaffDtos {
    private StaffDtos() {}

    public record RequestAccess(Integer areaId, Role role) {}

    public record GrantAccess(String email, Role role) {}

    public record Access(Integer id, Integer areaId, String areaName, String name, String email,
                         Role requestedRole, Role grantedRole, boolean personnel) {
        public static Access from(StaffAccess access) {
            return new Access(access.getId(), access.getArea().getId(), access.getArea().getName(),
                    access.getUser().getName(), access.getUser().getEmail(),
                    access.getRequestedRole(), access.getGrantedRole(), access.isPersonnel());
        }
    }
}
