
package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkins",
        indexes = {
                @Index(name = "idx_checkins_user", columnList = "user_id"),
                @Index(name = "idx_checkins_branch", columnList = "branch_id"),
                @Index(name = "idx_checkins_time", columnList = "check_in_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "check_in_at", nullable = false)
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @PrePersist
    void prePersist() {
        if (checkInAt == null) {
            checkInAt = LocalDateTime.now();
        }
    }

    public boolean isOpen() {
        return checkOutAt == null;
    }
}
