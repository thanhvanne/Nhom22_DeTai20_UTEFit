package com.utefit.controller.manager;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.service.ManagerAccessService;
import com.utefit.service.ManagerOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final ManagerAccessService access;
    private final ManagerOperationsService operations;

    @GetMapping
    public String index(@RequestParam(required = false) Long branchId, @RequestParam(defaultValue = "0") int page,
                        Principal principal, Model model) {
        var branches = access.myBranches(principal.getName());
        model.addAttribute("branches", branches);
        Long selected = branchId == null && !branches.isEmpty() ? branches.get(0).getId() : branchId;
        model.addAttribute("branchId", selected);
        if (selected != null) {
            model.addAttribute("history", operations.history(principal.getName(), selected, page));
            model.addAttribute("schedules", operations.schedules(principal.getName(), selected, null, page));
        }
        return "manager/index";
    }

    @PostMapping("/checkins")
    public String checkIn(@RequestParam Long branchId, @RequestParam String email, Principal principal, RedirectAttributes redirect) {
        try {
            operations.checkIn(principal.getName(), branchId, email);
            redirect.addFlashAttribute("success", "Check-in thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return home(branchId);
    }

    @PostMapping("/checkins/qr")
    public String scan(@RequestParam Long branchId, @RequestParam String qrToken, Principal principal, RedirectAttributes redirect) {
        try {
            operations.scan(principal.getName(), branchId, qrToken);
            redirect.addFlashAttribute("success", "Check-in QR thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return home(branchId);
    }

    @PostMapping("/checkins/{id}/checkout")
    public String checkOut(@PathVariable Long id, @RequestParam Long branchId, Principal principal, RedirectAttributes redirect) {
        try {
            operations.checkOut(principal.getName(), branchId, id);
            redirect.addFlashAttribute("success", "Checkout thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return home(branchId);
    }

    @GetMapping("/schedules/new")
    public String newSchedule(@RequestParam Long branchId, Principal principal, Model model) {
        model.addAttribute("scheduleForm", ClassScheduleFormDto.builder().scheduleDate(LocalDate.now().plusDays(1)).active(true).build());
        return form(principal.getName(), branchId, null, model);
    }

    @GetMapping("/schedules/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam Long branchId, Principal principal, Model model) {
        model.addAttribute("scheduleForm", operations.editForm(principal.getName(), branchId, id));
        return form(principal.getName(), branchId, id, model);
    }

    @PostMapping({"/schedules/new", "/schedules/{id}/edit"})
    public String save(@PathVariable(required = false) Long id, @RequestParam Long branchId,
                       @Valid @ModelAttribute("scheduleForm") ClassScheduleFormDto scheduleForm, BindingResult errors,
                       Principal principal, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                operations.saveSchedule(principal.getName(), branchId, id, scheduleForm);
                redirect.addFlashAttribute("success", "Đã lưu lịch lớp tại chi nhánh được giao.");
                return home(branchId);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return form(principal.getName(), branchId, id, model);
    }

    @PostMapping("/schedules/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam Long branchId, Principal principal, RedirectAttributes redirect) {
        try {
            operations.toggleSchedule(principal.getName(), branchId, id);
            redirect.addFlashAttribute("success", "Đã đổi trạng thái lịch lớp.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return home(branchId);
    }

    private String form(String email, Long branchId, Long id, Model model) {
        model.addAttribute("classes", operations.classes(email, branchId));
        model.addAttribute("branchId", branchId);
        model.addAttribute("editId", id);
        return "manager/schedule-form";
    }

    private String home(Long branchId) {
        return "redirect:/manager?branchId=" + branchId;
    }
}
