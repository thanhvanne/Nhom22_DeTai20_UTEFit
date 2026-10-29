package com.utefit.controller.admin;

import com.utefit.dto.admin.AdminUserResponse;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<AdminUserResponse> userPage = adminUserService.getUsers(keyword, role, status, page, size);

        model.addAttribute("activeMenu", "users");
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            AdminUserResponse user = adminUserService.getUserById(id);
            model.addAttribute("activeMenu", "users");
            model.addAttribute("user", user);
            return "admin/users/detail";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}/toggle-lock")
    public String toggleLock(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra
    ) {
        try {
            String currentEmail = (principal != null) ? principal.getName() : null;
            adminUserService.toggleLockUser(id, currentEmail);
            ra.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}