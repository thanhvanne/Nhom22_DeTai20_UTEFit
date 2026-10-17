package com.utefit.dto.trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerResponse {

    private Long id;

    private String fullName;

    private String avatarUrl;

    private String specialization;

    private Integer yearsExperience;

    private String bio;

    private Long branchId;

    private String branchName;

    private String branchAddress;
}