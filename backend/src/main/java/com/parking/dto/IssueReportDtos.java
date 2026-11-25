package com.parking.dto;

import com.parking.entity.IssueReport;
import com.parking.entity.IssueType;
import com.parking.entity.Reservation;
import com.parking.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

public final class IssueReportDtos {

    private IssueReportDtos() {}

    public record ResolveIssueRequest(boolean refund) {}

    public record AdminIssueReport(
            Integer id,
            Integer reservationId,
            String userId,
            String userName,
            String userEmail,
            String areaName,
            String spotNumber,
            IssueType type,
            String description,
            Instant reportedAt,
            Instant resolvedAt,
            String resolvedByName,
            boolean activeReservation,
            String currency,
            BigDecimal refundableAmount,
            BigDecimal refundAmount) {

        public static AdminIssueReport of(IssueReport report, Reservation reservation, User user,
                                          User resolver, Instant asOf) {
            BigDecimal available = reservation != null && report.getResolvedAt() == null
                    ? reservation.refundableAmount(asOf) : BigDecimal.ZERO;
            BigDecimal refund = reservation != null && report.getId().equals(reservation.getRefundReportId())
                    ? reservation.getRefundAmount() : null;
            return new AdminIssueReport(report.getId(), report.getReservationId(),
                    reservation != null ? reservation.getUserId() : null,
                    user != null ? user.getName() : null, user != null ? user.getEmail() : null,
                    report.getAreaName(), report.getSpotNumber(), report.getType(), report.getDescription(),
                    report.getReportedAt(), report.getResolvedAt(), resolver != null ? resolver.getName() : null,
                    reservation != null && reservation.isActive(),
                    reservation != null ? reservation.getCurrency() : null,
                    available.signum() > 0 ? available : null, refund);
        }
    }
}
