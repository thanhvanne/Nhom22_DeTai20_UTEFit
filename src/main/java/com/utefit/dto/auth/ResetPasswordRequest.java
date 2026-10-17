package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập OTP")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP phải gồm đúng 6 chữ số"
    )
    private String code;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(
            min = 8,
            max = 72,
            message = "Mật khẩu mới phải từ 8 đến 72 ký tự"
    )
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}