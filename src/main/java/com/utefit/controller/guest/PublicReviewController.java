package com.utefit.controller.guest;

import com.utefit.service.PublicCatalogService;
import com.utefit.service.PublicReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PublicReviewController {

    private final PublicReviewService reviews;
    private final PublicCatalogService catalog;

    @GetMapping("/reviews")
    public String list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "reviewPage",
                reviews.search(branchId, rating, page)
        );

        model.addAttribute(
                "branches",
                catalog.activeBranchOptions()
        );

        model.addAttribute("branchId", branchId);
        model.addAttribute("rating", rating);

        return "guest/reviews";
    }
}