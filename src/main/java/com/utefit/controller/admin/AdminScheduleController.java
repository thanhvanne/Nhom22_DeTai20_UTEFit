package com.utefit.controller.admin;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.ClassSchedule;
import com.utefit.service.AdminScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    @GetMapping
    public String listSchedules(
            @RequestParam(required = false) Long fitnessClassId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<ClassSchedule> schedulePage = adminScheduleService.getSchedules(
                fitnessClassId, branchId, scheduleDate, active, page, size
        );

        model.addAttribute("schedulePage", schedulePage);
        model.addAttribute("classes", adminScheduleService.getSchedulableClasses());
        model.addAttribute("branches", adminScheduleService.getAllBranches());

        model.addAttribute("selectedClassId", fitnessClassId);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedDate", scheduleDate);
        model.addAttribute("selectedActive", active);
        model.addAttribute("activeMenu", "schedules");

        return "admin/schedules/list";
    }

    @GetMapping("/create")
    public String showCreateForm(
            @RequestParam(required = false) Long classId,
            Model model
    ) {
        ClassScheduleFormDto form = ClassScheduleFormDto.builder()
                .fitnessClassId(classId)
                .scheduleDate(LocalDate.now().plusDays(1))
                .active(true)
                .build();

        model.addAttribute("scheduleForm", form);
        populateFormOptions(model, false);
        return "admin/schedules/form";
    }

    @PostMapping("/create")
    public String createSchedule(
            @Valid @ModelAttribute("scheduleForm") ClassScheduleFormDto scheduleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model, false);
            return "admin/schedules/form";
        }

        try {
            adminScheduleService.createSchedule(scheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Xếp lịch học mới thành công!");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormOptions(model, false);
            return "admin/schedules/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ClassScheduleFormDto scheduleForm = adminScheduleService.getScheduleFormById(id);
            model.addAttribute("scheduleForm", scheduleForm);
            populateFormOptions(model, true);
            return "admin/schedules/form";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/schedules";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateSchedule(
            @PathVariable Long id,
            @Valid @ModelAttribute("scheduleForm") ClassScheduleFormDto scheduleForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        scheduleForm.setId(id);
        if (bindingResult.hasErrors()) {
            populateFormOptions(model, true);
            return "admin/schedules/form";
        }

        try {
            adminScheduleService.updateSchedule(id, scheduleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch học thành công!");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormOptions(model, true);
            return "admin/schedules/form";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminScheduleService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái buổi học!");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    private void populateFormOptions(Model model, boolean isEdit) {
        model.addAttribute("classes", adminScheduleService.getSchedulableClasses());
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("activeMenu", "schedules");
    }
}