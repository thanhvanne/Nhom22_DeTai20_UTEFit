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
    @NotNull @Builder.Default private com.utefit.enums.MembershipDurationUnit durationUnit = com.utefit.enums.MembershipDurationUnit.DAY;
    @NotNull @Min(1) @Builder.Default @com.utefit.validation.ValidInput("durationQuantity")
    private Integer durationQuantity = 30;
    @NotNull @Builder.Default private com.utefit.enums.MembershipDurationUnit bonusUnit = com.utefit.enums.MembershipDurationUnit.DAY;
    @NotNull @Min(0) @Builder.Default @com.utefit.validation.ValidInput("bonusQuantity")
    private Integer bonusQuantity = 0;
    @NotNull @Builder.Default private com.utefit.enums.MembershipBenefit benefit = com.utefit.enums.MembershipBenefit.ALL;
    @NotNull @Builder.Default private com.utefit.enums.MembershipScope accessScope = com.utefit.enums.MembershipScope.SINGLE_BRANCH;
    @Builder.Default private java.util.Set<Long> branchIds = new java.util.HashSet<>();

    private Long id;

    @NotBlank(message = "Tên gói tập không được để trống")
    @Size(max = 100, message = "Tên gói tập tối đa 100 ký tự")
    private String name;

    @Size(max = 1500, message = "Mô tả tối đa 1500 ký tự")
    private String description;

    @Min(value = 1, message = "Thời hạn gói tập phải lớn hơn 0 ngày")
    @Max(value = 36600, message = "Thời hạn gói tối đa 36600 ngày")
    private Integer durationDays;

    @NotNull(message = "Vui lòng nhập giá gói tập")
    @DecimalMin(value = "0.01", inclusive = true, message = "Giá gói tập phải lớn hơn 0")
    @Digits(integer = 13, fraction = 0, message = "Giá VND phải là số nguyên, tối đa 13 chữ số")
    @com.utefit.validation.ValidInput("price")
    private BigDecimal price;

    @Builder.Default
    private Boolean active = true;
}
