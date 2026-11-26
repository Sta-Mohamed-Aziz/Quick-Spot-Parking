package com.parking.repository;

import com.parking.entity.Role;
import com.parking.entity.StaffAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAccessRepository extends JpaRepository<StaffAccess, Integer> {
    boolean existsByUser_IdAndGrantedRole(int userId, Role role);

    boolean existsByUser_IdAndPersonnelTrue(int userId);

    boolean existsByArea_IdAndUser_IdAndPersonnelTrue(int areaId, int userId);

    @EntityGraph(attributePaths = {"area", "user"})
    List<StaffAccess> findByArea_IdOrderByIdDesc(int areaId);

    @EntityGraph(attributePaths = {"area", "user"})
    List<StaffAccess> findByUser_IdOrderByIdDesc(int userId);

    Optional<StaffAccess> findByArea_IdAndUser_Id(int areaId, int userId);

    Optional<StaffAccess> findByIdAndArea_Id(int id, int areaId);
}
