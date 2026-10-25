package com.utefit.controller.auth;

import com.utefit.dto.auth.*;
import com.utefit.entity.User;
import com.utefit.service.AuthService;
import com.utefit.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.mail.MailException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Controller
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;
    private final boolean secureCookie;

    public AuthController(
            AuthService auth,
            JwtService jwt,
            @Value("${utefit.jwt.cookie-secure:false}")
            boolean secureCookie
    ) {
        this.auth = auth;
        this.jwt = jwt;
        this.secureCookie = secureCookie;
    }

    private String q(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    // =========================
    // REGISTER
    // =========================

    @GetMapping("/register")
    public String registerPage(Model model) {

        if (!model.containsAttribute("form")) {
            model.addAttribute(
                    "form",
                    new RegisterRequest()
            );
        }

        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterRequest form,
            BindingResult errors
    ) {
        if (errors.hasErrors()) {
            return "auth/register";
        }

        try {
            String email = auth.register(form);

            return "redirect:/verify-otp?email="
                    + q(email);

        } catch (IllegalArgumentException ex) {

            errors.reject(
                    "register",
                    ex.getMessage()
            );

            return "auth/register";
        }
    }

    // =========================
    // VERIFY OTP
    // =========================

    @GetMapping("/verify-otp")
    public String verifyPage(
            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String email,
            Model model
    ) {
        if (!model.containsAttribute("form")) {

            VerifyOtpRequest form =
                    new VerifyOtpRequest();

            form.setEmail(email);

            model.addAttribute("form", form);
        }

        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verify(
            @Valid @ModelAttribute("form")
            VerifyOtpRequest form,
            BindingResult errors,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            return "auth/verify-otp";
        }

        try {
            auth.verifyRegistration(form);

            redirect.addFlashAttribute(
                    "success",
                    "Xác thực thành công, hãy đăng nhập."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException ex) {

            errors.reject(
                    "otp",
                    ex.getMessage()
            );

            return "auth/verify-otp";
        }
    }

    // =========================
    // RESEND OTP
    // =========================

    @PostMapping("/resend-otp")
    public String resend(
            @RequestParam String email,
            RedirectAttributes redirect
    ) {
        try {
            auth.resendRegistrationOtp(email);

            redirect.addFlashAttribute(
                    "success",
                    "Nếu tài khoản đang chờ xác thực, OTP mới đã gửi."
            );

        } catch (IllegalArgumentException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/verify-otp?email="
                + q(email);
    }

    // =========================
    // LOGIN
    // =========================

    @GetMapping("/login")
    public String loginPage(Model model) {

        if (!model.containsAttribute("form")) {
            model.addAttribute(
                    "form",
                    new LoginRequest()
            );
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("form") LoginRequest form,
            BindingResult errors,
            HttpServletResponse response
    ) {
        if (errors.hasErrors()) {
            return "auth/login";
        }

        try {
            User user = auth.authenticate(
                    form.getEmail(),
                    form.getPassword()
            );

            String token = jwt.generate(user);

            ResponseCookie cookie =
                    ResponseCookie.from(
                                    "UTEFIT_AUTH",
                                    token
                            )
                            .httpOnly(true)
                            .secure(secureCookie)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(Duration.ofHours(2))
                            .build();

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    cookie.toString()
            );

            return "redirect:" + switch (user.getRole()) {
                case MEMBER -> "/member/dashboard";
                case PT -> "/pt/dashboard";
                case ADMIN -> "/admin/dashboard";
                case MANAGER -> "/manager";
            };

        } catch (AuthenticationException ex) {

            errors.reject(
                    "login",
                    "Sai email/mật khẩu hoặc tài khoản chưa kích hoạt."
            );

            return "auth/login";
        }
    }

    // =========================
    // LOGOUT
    // =========================

    @PostMapping("/auth/logout")
    public String logout(
            Authentication principal,
            HttpServletResponse response
    ) {
        if (principal != null && principal.isAuthenticated()) {
            auth.revokeTokensForUser(principal.getName());
        }

        ResponseCookie cookie = ResponseCookie.from("UTEFIT_AUTH", "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return "redirect:/login";
    }

    // =========================
    // FORGOT PASSWORD
    // =========================

    @GetMapping("/forgot-password")
    public String forgotPage(Model model) {

        if (!model.containsAttribute("form")) {

            model.addAttribute(
                    "form",
                    new ForgotPasswordRequest()
            );
        }

        return "auth/forgot-password";
    }


    @PostMapping("/forgot-password")
    public String forgot(
            @Valid @ModelAttribute("form")
            ForgotPasswordRequest form,
            BindingResult errors,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            auth.requestPasswordReset(
                    form.getEmail()
            );

            redirect.addFlashAttribute(
                    "success",
                    "Nếu tài khoản hợp lệ, mã OTP sẽ được gửi đến email."
            );

            return "redirect:/reset-password?email="
                    + q(form.getEmail());

        } catch (IllegalArgumentException ex) {

            errors.reject(
                    "forgot",
                    ex.getMessage()
            );

            return "auth/forgot-password";

        } catch (MailException ex) {

            errors.reject(
                    "mail",
                    "Không gửi được email OTP. Vui lòng thử lại sau."
            );

            return "auth/forgot-password";
        }
    }


    // =========================
    // RESET PASSWORD
    // =========================

    @GetMapping("/reset-password")
    public String resetPage(
            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String email,
            Model model
    ) {
        if (!model.containsAttribute("form")) {

            ResetPasswordRequest form =
                    new ResetPasswordRequest();

            form.setEmail(email);

            model.addAttribute("form", form);
        }

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(
            @Valid @ModelAttribute("form")
            ResetPasswordRequest form,
            BindingResult errors,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            auth.resetPassword(form);

            redirect.addFlashAttribute(
                    "success",
                    "Đổi mật khẩu thành công, hãy đăng nhập."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException ex) {

            errors.reject(
                    "reset",
                    ex.getMessage()
            );

            return "auth/reset-password";
        }
    }

    // =========================
    // ACCESS DENIED
    // =========================

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
