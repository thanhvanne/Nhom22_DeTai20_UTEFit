package com.utefit.dto.trainer;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalTrainingScheduleResponse {

    private Long id;

    private Long trainerId;
    private String trainerName;

    private Long memberId;
    private String memberName;

    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String title;
    private String note;

    // Lấy từ trainer.branch cho tiện hiển thị
    private String branchName;
    private String status;
}
