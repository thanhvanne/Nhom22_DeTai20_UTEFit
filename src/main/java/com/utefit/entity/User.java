package com.utefit.entity;

import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Nationalized
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "token_version")
    private Integer tokenVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (role == null) {
            role = UserRole.MEMBER;
        }

        if (status == null) {
            status = UserStatus.UNVERIFIED;
        }

        if (enabled == null) {
            enabled = false;
        }

        if (tokenVersion == null) {
            tokenVersion = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Thêm tiện ích full name cho các chỗ hiển thị tên ===
    public String getFullName() {
        String fn = firstName != null ? firstName : "";
        String ln = lastName != null ? lastName : "";
        return (fn + " " + ln).trim();
    }
}
