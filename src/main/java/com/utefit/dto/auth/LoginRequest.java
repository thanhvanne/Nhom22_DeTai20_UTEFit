package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @com.utefit.validation.ValidInput("email")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}
