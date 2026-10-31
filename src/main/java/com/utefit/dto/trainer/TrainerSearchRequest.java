package com.utefit.dto.trainer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerSearchRequest {

    @Size(max = 100)
    private String keyword;

    @com.utefit.validation.ValidInput("id")
    private Long branchId;

    @Min(value = 0)
    private Integer page = 0;

    @Min(value = 1)
    @Max(value = 6)
    private Integer size = 6;
}
