
package com.utefit.controller.member;

import com.utefit.dto.member.PasswordChangeRequest;
import com.utefit.dto.member.ProfileUpdateRequest;
import com.utefit.service.MemberProfileService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Duration;

@Controller
@RequestMapping("/member/profile")
public class MemberProfileController {

    private final MemberProfileService profiles;

    public MemberProfileController(
            MemberProfileService profiles
    ) {
        this.profiles = profiles;
    }

    @GetMapping
    public String showProfile(
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "profile",
                profiles.getProfile(principal.getName())
        );

        model.addAttribute(
                "profileForm",
                profiles.getEditForm(principal.getName())
        );

        model.addAttribute(
                "passwordForm",
                new PasswordChangeRequest()
        );

        return "member/profile";
    }

    @PostMapping
    public String updateProfile(
            @Valid @ModelAttribute("profileForm")
            ProfileUpdateRequest form,
            BindingResult result,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute(
                    "profile",
                    profiles.getProfile(principal.getName())
            );
            model.addAttribute(
                    "passwordForm",
                    new PasswordChangeRequest()
            );
            return "member/profile";
        }

        try {
            profiles.updateProfile(
                    principal.getName(),
                    form
            );
            redirect.addFlashAttribute(
                    "success",
                    "Cập nhật thông tin thành công"
            );
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/member/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm")
            PasswordChangeRequest form,
            BindingResult result,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute(
                    "profile",
                    profiles.getProfile(principal.getName())
            );
            model.addAttribute(
                    "profileForm",
                    profiles.getEditForm(principal.getName())
            );
            return "member/profile";
        }

        try {
            profiles.changePassword(
                    principal.getName(),
                    form
            );

            ResponseCookie cookie = ResponseCookie
                    .from("UTEFIT_AUTH", "")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    cookie.toString()
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
            return "redirect:/member/profile";
        }
    }

    @PostMapping("/avatar")
    public String changeAvatar(
            @RequestParam("avatar") MultipartFile file,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            profiles.updateAvatar(
                    principal.getName(),
                    file
            );
            redirect.addFlashAttribute(
                    "success",
                    "Cập nhật ảnh đại diện thành công"
            );
        } catch (RuntimeException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/member/profile";
    }
}
