package com.utefit.controller.admin;

import com.utefit.dto.admin.AutomaticPromotionForm;
import com.utefit.enums.AutomaticPromotionKind;
import com.utefit.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions/programs")
@RequiredArgsConstructor
public class AdminAutomaticPromotionController {
    private final AutomaticPromotionService programs;
    private final PromotionCatalogService catalog;

    @GetMapping("/{kind}")
    public String edit(@PathVariable String kind, Model model) {
        var type = kind(kind);
        model.addAttribute("programForm", programs.form(type));
        return editor(type, model);
    }

    @PostMapping("/{kind}")
    public String save(@PathVariable String kind, @jakarta.validation.Valid @ModelAttribute("programForm") AutomaticPromotionForm form,
                       BindingResult result, Model model, RedirectAttributes redirect) {
        var type = kind(kind);
        programs.errors(type, form).forEach((field, message) -> {
            if (!result.hasFieldErrors(field)) result.rejectValue(field, "program.invalid", message);
        });
        if (result.hasErrors()) return editor(type, model);
        try {
            programs.save(type, form);
            redirect.addFlashAttribute("success", "Đã lưu " + type + ". Cấu hình mới áp dụng cho đơn mới, không đổi giá đơn đang chờ.");
            return "redirect:/admin/promotions";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("program.save", exception.getMessage());
        } catch (DataAccessException exception) {
            result.reject("program.database", "Chưa lưu được cấu hình. Kiểm tra migration V717, quyền runtime hoặc tải lại nếu có người vừa chỉnh sửa.");
        }
        return editor(type, model);
    }

    @PostMapping("/{kind}/state")
    public String state(@PathVariable String kind, @RequestParam long version, @RequestParam boolean active, RedirectAttributes redirect) {
        var type = kind(kind);
        try {
            programs.setActive(type, version, active);
            redirect.addFlashAttribute("success", "Đã " + (active ? "bật " : "tạm dừng ") + type + ". Đơn đã chốt giữ nguyên giá và quyền lợi.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("error", "Không cập nhật được trạng thái. Kiểm tra schema/quyền runtime hoặc tải lại cấu hình mới nhất.");
        }
        return "redirect:/admin/promotions";
    }

    private String editor(AutomaticPromotionKind kind, Model model) {
        model.addAttribute("activeMenu", "promotions"); model.addAttribute("kind", kind.name());
        model.addAttribute("programRule", programs.settings().rule(kind));
        model.addAttribute("program", catalog.programs(null).stream().filter(program -> program.kind().equals(kind.name())).findFirst().orElseThrow());
        return "admin/promotions/program-form";
    }

    private AutomaticPromotionKind kind(String kind) {
        try { return AutomaticPromotionKind.valueOf(kind); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chương trình không hợp lệ."); }
    }
}
