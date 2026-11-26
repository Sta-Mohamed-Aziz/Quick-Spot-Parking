package com.parking.service;

import com.parking.dto.ParkingRequests.LayoutRequest;
import com.parking.dto.ParkingRequests.SpotUpdateRequest;
import com.parking.entity.ParkingArea;
import com.parking.entity.ParkingSpot;
import com.parking.entity.SpotPrice;
import com.parking.entity.SpotStatus;
import com.parking.entity.SpotType;
import com.parking.repository.ParkingAreaRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private static final SimpleGrantedAuthority ADMIN_AUTHORITY = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final UserRepository userRepository;

    public List<ParkingArea> getParkingAreas(Authentication caller) {
        Integer adminId = adminId(caller);
        return adminId == null ? parkingAreaRepository.findAll() : parkingAreaRepository.findManagedAreas(adminId);
    }

    public ParkingArea getParkingArea(int id, Authentication caller) {
        return findArea(id, adminId(caller));
    }

    @Transactional
    public ParkingArea createParkingArea(String name, int adminId) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The name is required");
        }
        if (parkingAreaRepository.existsByName(name.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A parking area with this name already exists");
        }
        ParkingArea area = new ParkingArea();
        area.setName(name.trim());
        area.setAdmin(userRepository.getReferenceById(adminId));
        return parkingAreaRepository.save(area);
    }

    public ParkingSpot getParkingSpot(int id, Authentication caller) {
        return findSpot(id, adminId(caller));
    }

    @Transactional
    public ParkingArea updateLayout(int areaId, int adminId, LayoutRequest request) {
        if (request == null || request.layout() == null || request.spots() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Layout and spots are required");
        }

        ParkingArea area = findArea(areaId, adminId);
        Map<String, SpotType> requestedSpots = new HashMap<>();
        List<SpotPrice> spotPricing = new ArrayList<>();
        for (LayoutRequest.Spot spot : request.spots()) {
            requestedSpots.put(spot.number(), spot.type());
            if (spot.pricePerHour() != null) {
                if (spot.pricePerHour().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "The price of spot " + spot.number() + " cannot be negative");
                }
                spotPricing.add(new SpotPrice(spot.number(), spot.pricePerHour()));
            }
        }

        for (ParkingSpot spot : List.copyOf(area.getSpots())) {
            if (!requestedSpots.containsKey(spot.getNumber())) {
                checkNotInUse(spot);
                area.getSpots().remove(spot);
            }
        }

        Map<String, ParkingSpot> existingSpots = new HashMap<>();
        for (ParkingSpot spot : area.getSpots()) {
            existingSpots.put(spot.getNumber(), spot);
        }

        requestedSpots.forEach((number, type) -> {
            if (existingSpots.containsKey(number)) {
                existingSpots.get(number).updateType(type);
            } else {
                area.getSpots().add(parkingSpotRepository.save(new ParkingSpot(number, type, area)));
            }
        });

        area.setLayout(request.layout());
        area.setSpotPricing(spotPricing);
        area.setCapacity(area.getSpots().size());
        return area;
    }

    @Transactional
    public ParkingSpot updateParkingSpot(int spotId, int adminId, SpotUpdateRequest request) {
        if (request == null || request.type() == null && request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one spot field must be provided");
        }

        ParkingSpot spot = findSpot(spotId, adminId);
        if (request.type() != null) {
            spot.updateType(request.type());
        }
        if (request.status() != null) {
            if (request.status() == SpotStatus.NOT_AVAILABLE) {
                checkNotInUse(spot);
            }
            spot.updateStatus(request.status());
        }
        return spot;
    }

    private ParkingArea findArea(int id, Integer adminId) {
        Optional<ParkingArea> area = adminId == null
                ? parkingAreaRepository.findById(id)
                : parkingAreaRepository.findManagedArea(id, adminId);
        return area.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found"));
    }

    private ParkingSpot findSpot(int id, Integer adminId) {
        Optional<ParkingSpot> spot = adminId == null
                ? parkingSpotRepository.findById(id)
                : parkingSpotRepository.findManagedSpot(id, adminId);
        return spot.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));
    }

    private static Integer adminId(Authentication caller) {
        return caller.getAuthorities().contains(ADMIN_AUTHORITY) ? (Integer) caller.getPrincipal() : null;
    }

    private void checkNotInUse(ParkingSpot spot) {
        if (spot.getStatus() == SpotStatus.RESERVED || spot.getStatus() == SpotStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Spot " + spot.getNumber() + " is reserved or occupied");
        }
    }
}
