package com.parking.service;

import com.parking.dto.StaffDtos.Access;
import com.parking.entity.ParkingArea;
import com.parking.entity.Role;
import com.parking.entity.StaffAccess;
import com.parking.entity.User;
import com.parking.repository.ParkingAreaRepository;
import com.parking.repository.StaffAccessRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffAccessService {
    private final ParkingAreaRepository areas;
    private final StaffAccessRepository access;
    private final UserRepository users;

    public List<ParkingAreaRepository.AreaName> requestableAreas() {
        return areas.findByAdminIsNotNullOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Access> requests(int userId) {
        return access.findByUser_IdOrderByIdDesc(userId).stream().map(Access::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Access> staff(int areaId, int adminId) {
        requireAreaAdmin(areaId, adminId);
        return access.findByArea_IdOrderByIdDesc(areaId).stream().map(Access::from).toList();
    }

    @Transactional
    public Access request(Integer areaId, int userId, Role role) {
        requireStaffRole(role);
        if (areaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a parking area");
        }
        ParkingArea area = lockArea(areaId);
        if (area.getAdmin() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found");
        }
        User user = activeUser(users.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")));
        StaffAccess entry = entryFor(area, user);
        if (entry.getGrantedRole() == null) {
            if (entry.getRequestedRole() != null && entry.getRequestedRole() != role) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this parking area is already pending");
            }
            entry.setRequestedRole(role);
        }
        return Access.from(access.save(entry));
    }

    @Transactional
    public Access approve(int areaId, int adminId, int accessId) {
        lockArea(areaId);
        requireAreaAdmin(areaId, adminId);
        StaffAccess entry = access.findByIdAndArea_Id(accessId, areaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Access request not found"));
        if (entry.getRequestedRole() != null) {
            activeUser(entry.getUser());
            grantRole(entry, entry.getRequestedRole());
        }
        return Access.from(entry);
    }

    @Transactional
    public Access grant(int areaId, int adminId, String email, Role role) {
        if (role != Role.PERSONNEL) {
            requireStaffRole(role);
        }
        ParkingArea area = lockArea(areaId);
        requireAreaAdmin(areaId, adminId);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the user's email");
        }
        User user = activeUser(users.findByEmailIgnoreCase(email.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for that email")));
        if (role != Role.PERSONNEL && area.getAdmin() != null && user.getId().equals(area.getAdmin().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account already owns the parking area");
        }
        StaffAccess entry = entryFor(area, user);
        if (role == Role.PERSONNEL) {
            entry.setPersonnel(true);
        } else {
            grantRole(entry, role);
        }
        return Access.from(access.save(entry));
    }

    private StaffAccess entryFor(ParkingArea area, User user) {
        return access.findByArea_IdAndUser_Id(area.getId(), user.getId())
                .orElseGet(() -> new StaffAccess(area, user));
    }

    private void requireAreaAdmin(int areaId, int adminId) {
        areas.findManagedArea(areaId, adminId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found"));
    }

    // Serialize changes to a parking area's staff so approvals and direct grants share one record.
    private ParkingArea lockArea(int areaId) {
        return areas.findForStaffUpdate(areaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found"));
    }

    private static void grantRole(StaffAccess entry, Role role) {
        if (entry.getGrantedRole() != Role.ADMIN) {
            entry.setGrantedRole(role);
        }
        if (entry.getGrantedRole() == Role.ADMIN || entry.getRequestedRole() == role) {
            entry.setRequestedRole(null);
        }
    }

    private static void requireStaffRole(Role role) {
        if (role != Role.ADMIN && role != Role.TICKET_COUNTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose administrator or ticket counter");
        }
    }

    private static User activeUser(User user) {
        if (user.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The account is blocked");
        }
        return user;
    }
}
