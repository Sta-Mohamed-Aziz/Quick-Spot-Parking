package com.parking.service;

import com.parking.dto.IssueReportDtos.AdminIssueReport;
import com.parking.entity.IssueReport;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.repository.IssueReportRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class IssueReportService {

    private final IssueReportRepository issueReports;
    private final ReservationRepository reservations;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<AdminIssueReport> list() {
        List<IssueReport> reports = issueReports.findAllByOrderByReportedAtDesc();
        List<Integer> reservationIds = reports.stream().map(IssueReport::getReservationId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, Reservation> stays = reservations.findAllById(reservationIds).stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));
        List<Integer> userIds = Stream.concat(stays.values().stream().map(IssueReportService::accountId),
                        reports.stream().map(IssueReport::getResolvedBy))
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, User> accounts = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Instant now = Instant.now();
        return reports.stream().map(report -> {
            Reservation stay = stays.get(report.getReservationId());
            return AdminIssueReport.of(report, stay, accounts.get(accountId(stay)),
                    accounts.get(report.getResolvedBy()), now);
        }).toList();
    }

    @Transactional
    public AdminIssueReport resolve(int reportId, int adminId, boolean refund) {
        IssueReport report = issueReports.findForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue report not found"));
        // Exit locks the same reservation so the credit and the final charge cannot overwrite each other.
        Reservation stay = report.getReservationId() == null ? null
                : reservations.findForUpdate(report.getReservationId()).orElse(null);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (report.getResolvedAt() == null) {
            if (refund) {
                BigDecimal credit = stay == null ? BigDecimal.ZERO : stay.refundableAmount(now);
                if (credit.signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation has no amount available to refund");
                }
                stay.applyRefund(reportId, credit);
            }
            report.resolve(adminId, now);
        } else if (refund && (stay == null || !report.getId().equals(stay.getRefundReportId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report has already been resolved without a refund");
        }
        return AdminIssueReport.of(report, stay, findUser(accountId(stay)), findUser(report.getResolvedBy()), now);
    }

    private User findUser(Integer id) {
        return id == null ? null : users.findById(id).orElse(null);
    }

    private static Integer accountId(Reservation stay) {
        if (stay == null || stay.getUserId() == null) {
            return null;
        }
        try {
            return Integer.valueOf(stay.getUserId());
        } catch (NumberFormatException e) {
            return null; // Older app versions used non-numeric reservation owners.
        }
    }
}
