
package com.utefit.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {

    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới ít nhất 8 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới")
    private String confirmPassword;
}
