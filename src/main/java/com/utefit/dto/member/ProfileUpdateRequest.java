
package com.utefit.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "Vui lòng nhập họ")
    @Size(max = 50, message = "Họ tối đa 50 ký tự")
    private String lastName;

    @NotBlank(message = "Vui lòng nhập tên")
    @Size(max = 50, message = "Tên tối đa 50 ký tự")
    private String firstName;

    @Pattern(
            regexp = "^(0[0-9]{9,10})?$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;
}
