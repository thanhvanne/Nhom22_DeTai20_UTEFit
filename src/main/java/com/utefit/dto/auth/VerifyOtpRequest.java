package com.utefit.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập OTP")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP phải gồm đúng 6 chữ số"
    )
    private String code;
}