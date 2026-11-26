package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "staff_access", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_staff_access_area_user", columnNames = {"area_id", "user_id"}))
public class StaffAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id")
    private ParkingArea area;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    private Role requestedRole;

    @Setter
    @Enumerated(EnumType.STRING)
    private Role grantedRole;

    public StaffAccess(ParkingArea area, User user) {
        this.area = area;
        this.user = user;
    }
}
