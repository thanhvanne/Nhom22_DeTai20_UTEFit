
package com.utefit.controller.admin;

import com.utefit.entity.Promotion;
import com.utefit.service.AdminPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final AdminPromotionService service;
    private final com.utefit.service.PromotionCatalogService catalog;
    private final com.utefit.repository.MembershipRepository memberships;
    private final com.utefit.service.AutomaticPromotionService automatic;

    @GetMapping
    public String index(
            @RequestParam(required = false) Long editId,
            Model model
    ) {
        model.addAttribute("activeMenu", "promotions");
        model.addAttribute("programs", catalog.programs(null));
        model.addAttribute("loyaltyEnabled", catalog.enabled());
        var settings = new java.util.LinkedHashMap<String, com.utefit.service.AutomaticPromotionService.Rule>();
        var programUsage = new java.util.LinkedHashMap<String, com.utefit.service.AutomaticPromotionService.Usage>();
        automatic.settings().rules().forEach((kind, rule) -> { settings.put(kind.name(), rule); programUsage.put(kind.name(), automatic.usage(kind)); });
        model.addAttribute("programSettings", settings); model.addAttribute("programUsage", programUsage);
        java.util.Map<Long, Long> usage = new java.util.HashMap<>();
        service.findAll().forEach(promotion -> usage.put(promotion.getId(), memberships.promotionReservations(promotion.getCode())));
        model.addAttribute("promotionUsage", usage);
        model.addAttribute("promotions", service.findAll());
        model.addAttribute("form", editId == null
                ? new Promotion() : service.findById(editId));
        model.addAttribute("editId", editId);
        return "admin/promotions/list";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @ModelAttribute Promotion form,
            RedirectAttributes ra
    ) {
        try {
            service.save(id, form);
            ra.addFlashAttribute("success",
                    "Đã lưu khuyến mãi thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            if (id != null) {
                return "redirect:/admin/promotions?editId=" + id;
            }
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes ra
    ) {
        try {
            service.toggle(id);
            ra.addFlashAttribute("success",
                    "Đã cập nhật trạng thái khuyến mãi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}
