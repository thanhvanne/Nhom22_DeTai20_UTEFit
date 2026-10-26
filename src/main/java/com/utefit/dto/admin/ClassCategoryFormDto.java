package com.utefit.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassCategoryFormDto {
    private com.utefit.enums.MembershipBenefit requiredBenefit;

    private Long id;

    @NotBlank(message = "Tên loại lớp không được để trống")
    @Size(max = 100, message = "Tên loại lớp tối đa 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @Builder.Default
    private Boolean active = true;
}
