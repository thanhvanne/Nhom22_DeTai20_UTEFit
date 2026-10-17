package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}