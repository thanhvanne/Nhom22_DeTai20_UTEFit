package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String firstName;

    @NotBlank(message = "Vui lòng nhập tên")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String lastName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    @Pattern(
            regexp = "^$|^[0-9+ .-]{9,20}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(
            min = 8,
            max = 72,
            message = "Mật khẩu phải từ 8 đến 72 ký tự"
    )
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}