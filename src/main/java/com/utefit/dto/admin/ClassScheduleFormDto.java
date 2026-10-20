package com.utefit.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassScheduleFormDto {

    private Long id;

    @NotNull(message = "Vui lòng chọn lớp tập")
    private Long fitnessClassId;

    @NotNull(message = "Vui lòng chọn ngày học")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @NotNull(message = "Vui lòng nhập giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng nhập giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 100, message = "Tên phòng tập tối đa 100 ký tự")
    private String room;

    @Builder.Default
    private Boolean active = true;
}