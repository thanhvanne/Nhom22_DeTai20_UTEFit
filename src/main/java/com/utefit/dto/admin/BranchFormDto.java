package com.utefit.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchFormDto {

    private Long id;
    private MultipartFile imageFile;
    private String imageUrl;
    private String imagePublicId;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(max = 150, message = "Tên chi nhánh tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Pattern(regexp = "^(|0[0-9]{9,10})$", message = "Số điện thoại không hợp lệ (10-11 số, bắt đầu bằng 0)")
    private String phone;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn giờ mở cửa")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @NotNull(message = "Vui lòng chọn giờ đóng cửa")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closingTime;

    @Builder.Default
    private Boolean active = true;
}