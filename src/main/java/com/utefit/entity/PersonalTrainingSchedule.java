package com.utefit.entity;

import jakarta.persistence.*;
import com.utefit.enums.PersonalTrainingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "personal_training_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalTrainingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PT phụ trách buổi tập
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trainer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pts_trainer")
    )
    private Trainer trainer;

    // Member tham gia buổi tập
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pts_member")
    )
    private User member;

    // Chi nhánh gốc tại thời điểm tạo lịch, không suy luận theo PT.branch thay đổi về sau.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // Nullable để Hibernate đọc được lịch cũ trước khi chạy migration backfill.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PersonalTrainingStatus status;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 255)
    private String title;

    @Column(length = 2000)
    private String note;

    @Column(nullable = false)
    private Boolean active;

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

        if (active == null) {
            active = true;
        }
        if (status == null) status = PersonalTrainingStatus.SCHEDULED;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
