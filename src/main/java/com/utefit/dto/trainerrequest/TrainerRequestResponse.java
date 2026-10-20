package com.utefit.dto.trainerrequest;

import com.utefit.enums.TrainerRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerRequestResponse {

    private Long id;

    private Long memberId;
    private String memberName;
    private String memberEmail;

    private Long trainerId;
    private String trainerName;
    private String trainerEmail;

    private String message;

    private TrainerRequestStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}