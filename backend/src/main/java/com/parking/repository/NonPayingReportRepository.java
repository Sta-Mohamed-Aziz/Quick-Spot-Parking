package com.parking.repository;

import com.parking.entity.NonPayingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NonPayingReportRepository extends JpaRepository<NonPayingReport, Integer> {

    List<NonPayingReport> findAllByOrderByReportedAtDesc();

    @Query("""
            select report from NonPayingReport report where report.reservationId in
                (select r.id from Reservation r where r.areaId in :areaIds)
            order by report.reportedAt desc
            """)
    List<NonPayingReport> findAreaReports(@Param("areaIds") List<Integer> areaIds);

    boolean existsByReservationId(Integer reservationId);

    List<NonPayingReport> findByReservationIdIn(Collection<Integer> reservationIds);

    List<NonPayingReport> findByUserId(Integer userId);
}
