package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@com.utefit.validation.ConsistentForm
public class ResetPasswordRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @com.utefit.validation.ValidInput("email")
    private String email;

    @NotBlank(message = "Vui lòng nhập OTP")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP phải gồm đúng 6 chữ số"
    )
    @com.utefit.validation.ValidInput("code")
    private String code;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(
            min = 8,
            max = 72,
            message = "Mật khẩu mới phải từ 8 đến 72 ký tự"
    )
    @com.utefit.validation.ValidInput("newPassword")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @com.utefit.validation.ValidInput("confirmPassword")
    private String confirmPassword;
}
