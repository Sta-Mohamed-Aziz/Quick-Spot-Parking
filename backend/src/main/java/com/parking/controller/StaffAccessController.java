package com.parking.controller;

import com.parking.dto.StaffDtos.Access;
import com.parking.dto.StaffDtos.GrantAccess;
import com.parking.dto.StaffDtos.RequestAccess;
import com.parking.repository.ParkingAreaRepository.AreaName;
import com.parking.service.StaffAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StaffAccessController {
    private final StaffAccessService staffAccess;

    @GetMapping("/staff-access/areas")
    public List<AreaName> requestableAreas() {
        return staffAccess.requestableAreas();
    }

    @GetMapping("/staff-access/requests")
    public List<Access> requests(@AuthenticationPrincipal Integer userId) {
        return staffAccess.requests(userId);
    }

    @PostMapping("/staff-access/requests")
    public Access request(@AuthenticationPrincipal Integer userId, @RequestBody RequestAccess request) {
        return staffAccess.request(request.areaId(), userId, request.role());
    }

    @GetMapping("/admin/areas/{areaId}/staff")
    public List<Access> staff(@PathVariable int areaId, @AuthenticationPrincipal Integer adminId) {
        return staffAccess.staff(areaId, adminId);
    }

    @PostMapping("/admin/areas/{areaId}/staff")
    public Access grant(@PathVariable int areaId, @AuthenticationPrincipal Integer adminId,
                        @RequestBody GrantAccess grant) {
        return staffAccess.grant(areaId, adminId, grant.email(), grant.role());
    }

    @PostMapping("/admin/areas/{areaId}/staff/{accessId}/approve")
    public Access approve(@PathVariable int areaId, @PathVariable int accessId,
                          @AuthenticationPrincipal Integer adminId) {
        return staffAccess.approve(areaId, adminId, accessId);
    }
}
