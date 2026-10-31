
package com.utefit.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@com.utefit.validation.ConsistentForm
public class PasswordChangeRequest {

    @NotBlank(message = "Nhập mật khẩu hiện tại")
    @com.utefit.validation.ValidInput("currentPassword")
    private String currentPassword;

    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới ít nhất 8 ký tự")
    @com.utefit.validation.ValidInput("newPassword")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới")
    @com.utefit.validation.ValidInput("confirmPassword")
    private String confirmPassword;
}
