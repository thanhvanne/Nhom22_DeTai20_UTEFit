package com.utefit.dto.admin;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlanFormDto {

    private Long id;

    @NotBlank(message = "Tên gói tập không được để trống")
    @Size(max = 100, message = "Tên gói tập tối đa 100 ký tự")
    private String name;

    @Size(max = 1500, message = "Mô tả tối đa 1500 ký tự")
    private String description;

    @NotNull(message = "Vui lòng nhập thời hạn gói tập (số ngày)")
    @Min(value = 1, message = "Thời hạn gói tập phải lớn hơn 0 ngày")
    private Integer durationDays;

    @NotNull(message = "Vui lòng nhập giá gói tập")
    @DecimalMin(value = "0.01", inclusive = true, message = "Giá gói tập phải lớn hơn 0")
    private BigDecimal price;

    @Builder.Default
    private Boolean active = true;
}