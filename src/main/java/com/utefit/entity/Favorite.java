package com.utefit.entity;

import com.utefit.enums.FavoriteType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(name = "uk_favorite_member_target",
        columnNames = {"user_id", "target_type", "target_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private FavoriteType targetType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void created() {
        createdAt = LocalDateTime.now();
    }
}
