package com.parking.repository;

import com.parking.entity.ParkingArea;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingAreaRepository extends JpaRepository<ParkingArea, Integer> {
    String MANAGED_BY = """
            (a.admin.id = :userId or exists (
                select access.id from StaffAccess access where access.area.id = a.id and access.user.id = :userId
                and access.grantedRole = com.parking.entity.Role.ADMIN
            ))
            """;

    boolean existsByName(String name);

    @Query("select a from ParkingArea a where " + MANAGED_BY)
    List<ParkingArea> findManagedAreas(@Param("userId") int userId);

    @Query("select a from ParkingArea a where a.id = :id and " + MANAGED_BY)
    Optional<ParkingArea> findManagedArea(@Param("id") int id, @Param("userId") int userId);

    @Query("""
            select a.id from ParkingArea a where a.admin.id = :userId or exists (
                select access.id from StaffAccess access where access.area.id = a.id
                and access.user.id = :userId and access.grantedRole is not null
            )
            """)
    List<Integer> findCounterAreaIds(@Param("userId") int userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ParkingArea a where a.id = :id")
    Optional<ParkingArea> findForStaffUpdate(@Param("id") int id);

    List<AreaName> findByAdminIsNotNullOrderByNameAsc();

    interface AreaName {
        Integer getId();
        String getName();
    }
}
