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
    private final com.utefit.service.PromotionCatalogService catalog;
    @GetMapping("/promotions")
    @Transactional(readOnly = true)
    public String list(Model model, org.springframework.security.core.Authentication authentication) {
        String email = authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MEMBER")) ? authentication.getName() : null;
        model.addAttribute("programs", catalog.programs(email));
        model.addAttribute("personalized", email != null);
        java.util.Map<Long, Long> usage = new java.util.HashMap<>();
        if (email != null) promotions.findAllByOrderByIdDesc().forEach(promotion -> usage.put(promotion.getId(), catalog.uses(email, promotion.getCode())));
        model.addAttribute("promotionUsage", usage);
        LocalDate today = LocalDate.now(com.utefit.service.SepayPaymentService.ZONE);
        model.addAttribute("promotions", promotions.findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndQuantityGreaterThanOrderByEndDateAsc(today, today, 0).stream().filter(p -> p.getDiscountPercent().signum() > 0 && p.getDiscountPercent().compareTo(java.math.BigDecimal.valueOf(100)) < 0).toList());
        return "guest/promotions";
    }
}
