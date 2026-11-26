package com.parking.controller;

import com.parking.dto.ParkingRequests.CreateParkingAreaRequest;
import com.parking.dto.ParkingRequests.LayoutRequest;
import com.parking.entity.ParkingArea;
import com.parking.external.WeatherService;
import com.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/parking-areas")
@RequiredArgsConstructor
public class ParkingAreaController {

    private final ParkingService parkingService;
    private final WeatherService weatherService;

    @GetMapping
    public List<ParkingArea> getParkingAreas(Authentication caller) {
        return parkingService.getParkingAreas(caller);
    }

    @GetMapping("/{id}")
    public ParkingArea getParkingArea(@PathVariable int id, Authentication caller) {
        return parkingService.getParkingArea(id, caller);
    }

    /** Current weather at the area, from the external Open-Meteo service. */
    @GetMapping("/{id}/weather")
    public WeatherService.AreaWeather getWeather(@PathVariable int id, Authentication caller) {
        return weatherService.getWeather(parkingService.getParkingArea(id, caller));
    }

    @PostMapping
    public ResponseEntity<ParkingArea> createParkingArea(@RequestBody CreateParkingAreaRequest request,
                                                         @AuthenticationPrincipal Integer adminId) {
        ParkingArea area = parkingService.createParkingArea(request.name(), adminId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(area.getId())
                .toUri();
        return ResponseEntity.created(location).body(area);
    }

    @PutMapping("/{id}/layout")
    public ParkingArea updateLayout(@PathVariable int id, @RequestBody LayoutRequest request,
                                    @AuthenticationPrincipal Integer adminId) {
        return parkingService.updateLayout(id, adminId, request);
    }
}
