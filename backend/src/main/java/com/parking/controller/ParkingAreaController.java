package com.parking.controller;

import com.parking.dto.ParkingAreaDetails;
import com.parking.dto.ParkingRequests.CreateParkingAreaRequest;
import com.parking.dto.ParkingRequests.LayoutRequest;
import com.parking.entity.ParkingArea;
import com.parking.entity.SpotTypeRates;
import com.parking.external.WeatherService;
import com.parking.service.ParkingService;
import com.parking.service.ReservationService;
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
    private final ReservationService reservationService;
    private final WeatherService weatherService;

    @GetMapping
    public List<ParkingArea> getParkingAreas(Authentication caller) {
        return parkingService.getParkingAreas(caller);
    }

    @GetMapping("/{id}")
    public ParkingAreaDetails getParkingArea(@PathVariable int id, Authentication caller,
                                             @AuthenticationPrincipal Integer accountId) {
        ParkingArea area = parkingService.getParkingArea(id, caller);
        return new ParkingAreaDetails(area, reservationService.bookingRates(area, accountId));
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

    @PutMapping("/{id}/rates")
    public SpotTypeRates updateRates(@PathVariable int id, @RequestBody SpotTypeRates rates,
                                     @AuthenticationPrincipal Integer adminId) {
        return parkingService.updateRates(id, adminId, rates);
    }
}
