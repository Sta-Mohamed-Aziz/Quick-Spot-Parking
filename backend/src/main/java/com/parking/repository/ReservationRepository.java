package com.parking.repository;

import com.parking.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findForUpdate(@Param("id") int id);

    boolean existsByUserIdAndEndedAtIsNull(String userId);

    boolean existsBySpotIdAndEndedAtIsNull(Integer spotId);

    List<Reservation> findByUserIdOrderByStartedAtDesc(String userId);

    /** The most recent reservations, for the ticket counter. */
    List<Reservation> findTop100ByOrderByStartedAtDesc();

    List<Reservation> findTop100ByAreaIdInOrderByStartedAtDesc(List<Integer> areaIds);

    /** Paid stays (earnings are counted on the exit date). */
    @Query("""
            select r from Reservation r
            where r.endedAt >= :from
              and r.areaId in (select a.id from ParkingArea a where
            """ + ParkingAreaRepository.MANAGED_BY + ")")
    List<Reservation> findEarningsByAdmin(@Param("userId") int adminId, @Param("from") Instant from);

    /** Entries (a reservation starts when the driver enters). */
    @Query("""
            select r from Reservation r
            where r.startedAt >= :from
              and r.areaId in (select a.id from ParkingArea a where
            """ + ParkingAreaRepository.MANAGED_BY + ")")
    List<Reservation> findEntriesByAdmin(@Param("userId") int adminId, @Param("from") Instant from);
}
