package com.utefit.dto.admin;

import com.utefit.enums.ClassStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessClassFormDto {

    private Long id;

    @NotBlank(message = "Tên lớp tập không được để trống")
    @Size(max = 150, message = "Tên lớp tập tối đa 150 ký tự")
    private String name;

    @Size(max = 1500, message = "Mô tả tối đa 1500 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn chi nhánh tổ chức lớp tập")
    @com.utefit.validation.ValidInput("id")
    private Long branchId;

    @NotNull(message = "Vui lòng chọn danh mục loại lớp")
    @com.utefit.validation.ValidInput("id")
    private Long categoryId;

    // PT có thể chọn hoặc để trống (nhưng nếu chọn thì bắt buộc PT phải ACTIVE)
    @com.utefit.validation.ValidInput("id")
    private Long trainerId;

    @NotNull(message = "Vui lòng nhập sức chứa tối đa (số học viên)")
    @Min(value = 1, message = "Sức chứa lớp tập phải lớn hơn 0")
    @com.utefit.validation.ValidInput("capacity")
    private Integer capacity;

    @Builder.Default
    private ClassStatus status = ClassStatus.OPEN;
}
