package com.utefit.dto.trainer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerProfileUpdateRequest {


    @Size(
            max = 255,
            message = "Chuyên môn không được vượt quá 255 ký tự"
    )
    private String specialization;

    @Min(
            value = 0,
            message = "Số năm kinh nghiệm không được âm"
    )
    @Max(
            value = 70,
            message = "Số năm kinh nghiệm không hợp lệ"
    )
    @com.utefit.validation.ValidInput("yearsExperience")
    private Integer yearsExperience;

    @Size(
            max = 2000,
            message = "Giới thiệu không được vượt quá 2000 ký tự"
    )
    private String bio;
}
