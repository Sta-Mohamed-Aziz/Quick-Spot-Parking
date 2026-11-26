package com.parking.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.parking.entity.ParkingArea;

import java.math.BigDecimal;
import java.util.Map;

public record ParkingAreaDetails(@JsonUnwrapped ParkingArea area, BookingRates bookingRates) {
    /** Current prices by spot ID. Spots reserved for other personnel have no rate. */
    public record BookingRates(String currency, Map<Integer, BigDecimal> hourlyRates) {}
}
