package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;
}