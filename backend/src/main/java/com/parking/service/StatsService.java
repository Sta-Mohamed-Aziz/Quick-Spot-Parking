package com.parking.service;

import com.parking.dto.StatsDtos.AreaEntries;
import com.parking.dto.StatsDtos.DailyEntries;
import com.parking.dto.StatsDtos.EarningsBucket;
import com.parking.dto.StatsDtos.EarningsStats;
import com.parking.dto.StatsDtos.EntryStats;
import com.parking.dto.StatsDtos.Period;
import com.parking.entity.ParkingArea;
import com.parking.entity.Reservation;
import com.parking.repository.ParkingAreaRepository;
import com.parking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** SCRUM-80 (daily entries per parking area) and SCRUM-81 (daily, monthly and yearly earnings). */
@Service
public class StatsService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final ReservationRepository reservationRepository;
    private final ParkingAreaRepository parkingAreaRepository;
    private final String currency;
    private final ZoneId zone;

    public StatsService(ReservationRepository reservationRepository,
                        ParkingAreaRepository parkingAreaRepository,
                        @Value("${parking.currency}") String currency,
                        @Value("${parking.time-zone:Africa/Tunis}") String timeZone) {
        this.reservationRepository = reservationRepository;
        this.parkingAreaRepository = parkingAreaRepository;
        this.currency = currency;
        this.zone = ZoneId.of(timeZone);
    }

    /** Last 30 days, 12 months or 5 years, oldest first, with today / this month / this year totals. */
    public EarningsStats earnings(int adminId, Period period) {
        LocalDate today = LocalDate.now(zone);
        List<LocalDate> starts = new ArrayList<>();
        switch (period) {
            case DAY -> { for (int i = 29; i >= 0; i--) starts.add(today.minusDays(i)); }
            case MONTH -> { for (int i = 11; i >= 0; i--) starts.add(today.withDayOfMonth(1).minusMonths(i)); }
            case YEAR -> { for (int i = 4; i >= 0; i--) starts.add(today.withDayOfYear(1).minusYears(i)); }
        }
        LocalDate from = starts.get(0).isBefore(today.withDayOfYear(1)) ? starts.get(0) : today.withDayOfYear(1);

        List<Reservation> paid = reservationRepository.findEarningsByAdmin(adminId, startOf(from)).stream()
                .filter(r -> r.getAmount() != null && !r.isPaymentExempt())
                .toList();

        Map<LocalDate, List<Reservation>> byBucket = paid.stream()
                .collect(Collectors.groupingBy(r -> bucketOf(dateOf(r.getEndedAt()), period)));
        List<EarningsBucket> buckets = starts.stream()
                .map(start -> {
                    List<Reservation> in = byBucket.getOrDefault(start, List.of());
                    return new EarningsBucket(start.toString(), label(start, period), sum(in), in.size());
                })
                .toList();

        return new EarningsStats(
                period,
                currency,
                sum(paid.stream().filter(r -> dateOf(r.getEndedAt()).equals(today)).toList()),
                sum(paid.stream().filter(r -> bucketOf(dateOf(r.getEndedAt()), Period.MONTH).equals(today.withDayOfMonth(1))).toList()),
                sum(paid.stream().filter(r -> dateOf(r.getEndedAt()).getYear() == today.getYear()).toList()),
                buckets);
    }

    /** Entries per day over the last {@code days} days, for one owned area or all areas owned by the admin. */
    public EntryStats entries(int adminId, Integer areaId, int days) {
        List<ParkingArea> ownedAreas = parkingAreaRepository.findManagedAreas(adminId);
        if (areaId != null && ownedAreas.stream().noneMatch(area -> areaId.equals(area.getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found");
        }

        int span = Math.max(1, Math.min(days, 366));
        LocalDate today = LocalDate.now(zone);
        LocalDate first = today.minusDays(span - 1L);

        List<Reservation> entered = reservationRepository.findEntriesByAdmin(adminId, startOf(first));
        List<Reservation> selected = entered.stream()
                .filter(r -> areaId == null || areaId.equals(r.getAreaId()))
                .toList();

        Map<LocalDate, Long> perDay = selected.stream()
                .collect(Collectors.groupingBy(r -> dateOf(r.getStartedAt()), Collectors.counting()));
        List<DailyEntries> daily = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            daily.add(new DailyEntries(d.toString(), d.format(DAY_LABEL), perDay.getOrDefault(d, 0L)));
        }
        DailyEntries peak = daily.stream()
                .filter(d -> d.entries() > 0)
                .max(Comparator.comparingLong(DailyEntries::entries))
                .orElse(null);

        // Include owned areas with zero entries so the admin can compare them.
        Map<Integer, List<Reservation>> byArea = entered.stream()
                .collect(Collectors.groupingBy(Reservation::getAreaId));
        List<AreaEntries> areas = ownedAreas.stream()
                .map(area -> {
                    List<Reservation> in = byArea.getOrDefault(area.getId(), List.of());
                    long todayCount = in.stream().filter(r -> dateOf(r.getStartedAt()).equals(today)).count();
                    return new AreaEntries(area.getId(), area.getName(), todayCount, in.size(), round((double) in.size() / span));
                })
                .sorted(Comparator.comparingLong(AreaEntries::total).reversed().thenComparing(AreaEntries::areaName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new EntryStats(
                areaId,
                span,
                perDay.getOrDefault(today, 0L),
                round((double) selected.size() / span),
                peak,
                daily,
                areas);
    }

    private LocalDate dateOf(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    private Instant startOf(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    private static LocalDate bucketOf(LocalDate date, Period period) {
        return switch (period) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private static String label(LocalDate start, Period period) {
        return switch (period) {
            case DAY -> start.format(DAY_LABEL);
            case MONTH -> start.format(MONTH_LABEL);
            case YEAR -> String.valueOf(start.getYear());
        };
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        return reservations.stream()
                .map(Reservation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
