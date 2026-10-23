package com.utefit.controller.admin;

import com.utefit.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService service;

    @GetMapping
    public String list(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("activeMenu", "reviews");
        model.addAttribute("branches",
                service.listBranches());
        model.addAttribute("reviews",
                service.search(rating, branchId, page));
        model.addAttribute("rating", rating);
        model.addAttribute("branchId", branchId);
        return "admin/reviews/list";
    }

    @PostMapping("/{id}/toggle-visibility")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes ra
    ) {
        try {
            service.toggleVisibility(id);
            ra.addFlashAttribute("success",
                    "Đã cập nhật trạng thái hiển thị đánh giá.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}
