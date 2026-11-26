package com.parking.entity;

import java.math.BigDecimal;

/** Optional hourly rates for the paid spot types in an area. */
public record SpotTypeRates(BigDecimal regular, BigDecimal ev, BigDecimal handicapped) {
    public BigDecimal rateFor(SpotType type) {
        return switch (type) {
            case REGULAR -> regular;
            case EV -> ev;
            case HANDICAPPED -> handicapped;
            case PERSONNEL -> null;
        };
    }
}
