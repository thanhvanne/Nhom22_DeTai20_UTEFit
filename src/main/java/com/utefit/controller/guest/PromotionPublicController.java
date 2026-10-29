package com.utefit.controller.guest;

import com.utefit.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class PromotionPublicController {
    private final PromotionRepository promotions;
    @GetMapping("/promotions")
    @Transactional(readOnly = true)
    public String list(Model model) {
        LocalDate today = LocalDate.now(com.utefit.service.SepayPaymentService.ZONE);
        model.addAttribute("promotions", promotions.findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndQuantityGreaterThanOrderByEndDateAsc(today, today, 0).stream().filter(p -> p.getDiscountPercent().signum() > 0 && p.getDiscountPercent().compareTo(java.math.BigDecimal.valueOf(100)) < 0).toList());
        return "guest/promotions";
    }
}
