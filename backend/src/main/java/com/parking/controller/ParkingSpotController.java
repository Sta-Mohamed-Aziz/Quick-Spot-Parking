package com.parking.controller;

import com.parking.dto.ParkingRequests.SpotUpdateRequest;
import com.parking.entity.ParkingSpot;
import com.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking-spots")
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingService parkingService;

    @GetMapping("/{id}")
    public ParkingSpot getParkingSpot(@PathVariable int id, Authentication caller) {
        return parkingService.getParkingSpot(id, caller);
    }

    @PatchMapping("/{id}")
    public ParkingSpot updateParkingSpot(@PathVariable int id, @RequestBody SpotUpdateRequest request,
                                         @AuthenticationPrincipal Integer adminId) {
        return parkingService.updateParkingSpot(id, adminId, request);
    }
}
