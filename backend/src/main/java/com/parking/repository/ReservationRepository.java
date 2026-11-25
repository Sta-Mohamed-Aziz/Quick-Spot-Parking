package com.parking.repository;

import com.parking.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Paid stays (earnings are counted on the exit date). */
    List<Reservation> findByEndedAtGreaterThanEqual(java.time.Instant from);

    /** Entries (a reservation starts when the driver enters). */
    List<Reservation> findByStartedAtGreaterThanEqual(java.time.Instant from);
}
