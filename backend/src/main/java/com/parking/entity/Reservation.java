package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

/**
 * A user's stay on a spot, from reservation to exit. Once ended it is the receipt.
 * Spot and area details are copied so the receipt stays the same if the layout changes later.
 */
@Entity
@Getter
@NoArgsConstructor
@Table (name="reservation" , schema="public")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String userId;

    private Integer spotId;

    private String spotNumber;

    private Integer areaId;

    private String areaName;

    private Instant startedAt;

    private Instant endedAt;

    @Column(precision = 10, scale = 3)
    private BigDecimal pricePerHour;

    @Column(precision = 10, scale = 3)
    private BigDecimal amount;

    @Column(precision = 10, scale = 3)
    private BigDecimal refundAmount;

    @JsonIgnore
    private Integer refundReportId;

    private String currency;

    public Reservation(String userId, ParkingSpot spot, BigDecimal pricePerHour, String currency) {
        this.userId = userId;
        this.spotId = spot.getId();
        this.spotNumber = spot.getNumber();
        this.areaId = spot.getParkingArea().getId();
        this.areaName = spot.getParkingArea().getName();
        this.pricePerHour = pricePerHour;
        this.currency = currency;
        this.startedAt = now();
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean isPaymentExempt() {
        return pricePerHour != null && pricePerHour.signum() == 0;
    }

    /** Started minutes, at least one: 0:30 is 1 minute, 1:01 is 2 minutes. */
    public long getMinutes() {
        return minutesAt(endedAt != null ? endedAt : Instant.now());
    }

    private long minutesAt(Instant end) {
        long seconds = Duration.between(startedAt, end).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    public void end() {
        endedAt = now();
        amount = chargeAt(endedAt);
        if (refundAmount != null) {
            amount = amount.subtract(refundAmount).max(BigDecimal.ZERO);
        }
    }

    public BigDecimal refundableAmount(Instant asOf) {
        if (refundAmount != null) {
            return BigDecimal.ZERO;
        }
        BigDecimal charge = isActive() ? chargeAt(asOf) : amount;
        return charge == null ? BigDecimal.ZERO : charge.max(BigDecimal.ZERO);
    }

    public void applyRefund(int reportId, BigDecimal credit) {
        refundReportId = reportId;
        refundAmount = credit;
        if (!isActive()) {
            amount = amount.subtract(credit);
        }
    }

    private BigDecimal chargeAt(Instant end) {
        return pricePerHour.multiply(BigDecimal.valueOf(minutesAt(end)))
                .divide(BigDecimal.valueOf(60), decimals(currency), RoundingMode.HALF_UP);
    }

    /** Microseconds, like the database column, so the receipt returned at exit equals the stored one. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static int decimals(String currency) {
        try {
            return Math.max(0, Currency.getInstance(currency).getDefaultFractionDigits());
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }
}
