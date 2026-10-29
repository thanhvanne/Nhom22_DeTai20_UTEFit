package com.utefit.controller.admin;

import com.utefit.service.ManagerAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/admin/managers")
@RequiredArgsConstructor
public class AdminManagerController {
    private final ManagerAccessService access;

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("managers", access.managers(principal.getName()));
        model.addAttribute("branches", access.availableBranches(principal.getName()));
        model.addAttribute("activeMenu", "managers");
        return "admin/managers";
    }

    @PostMapping
    public String create(@RequestParam String email, @RequestParam String name, @RequestParam String password,
                         Principal principal, RedirectAttributes redirect) {
        try {
            access.createManager(principal.getName(), email, name, password);
            redirect.addFlashAttribute("success", "Đã tạo Manager mới; hãy phân công chi nhánh.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/managers";
    }

    @PostMapping("/{id}/branches")
    public String grant(@PathVariable Long id, @RequestParam Long branchId, Principal principal, RedirectAttributes redirect) {
        try {
            access.grant(principal.getName(), id, branchId);
            redirect.addFlashAttribute("success", "Đã phân công chi nhánh.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/managers";
    }

    @PostMapping("/{id}/branches/{branchId}/remove")
    public String revoke(@PathVariable Long id, @PathVariable Long branchId, Principal principal, RedirectAttributes redirect) {
        access.revoke(principal.getName(), id, branchId);
        redirect.addFlashAttribute("success", "Đã thu hồi quyền tại chi nhánh; có hiệu lực ở yêu cầu tiếp theo.");
        return "redirect:/admin/managers";
    }
}
