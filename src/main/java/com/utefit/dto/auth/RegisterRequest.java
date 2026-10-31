package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@com.utefit.validation.ConsistentForm
public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    @com.utefit.validation.ValidInput("firstName")
    private String firstName;

    @NotBlank(message = "Vui lòng nhập tên")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    @com.utefit.validation.ValidInput("lastName")
    private String lastName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    @com.utefit.validation.ValidInput("email")
    private String email;

    @com.utefit.validation.ValidInput("phone")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(
            min = 8,
            max = 72,
            message = "Mật khẩu phải từ 8 đến 72 ký tự"
    )
    @com.utefit.validation.ValidInput("password")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @com.utefit.validation.ValidInput("confirmPassword")
    private String confirmPassword;
}
