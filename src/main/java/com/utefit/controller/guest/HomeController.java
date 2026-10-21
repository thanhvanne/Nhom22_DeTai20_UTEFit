package com.utefit.controller.guest;

import com.utefit.service.PublicCatalogService;
import com.utefit.service.PublicReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PublicCatalogService catalog;
    private final PublicReviewService reviews;

    public HomeController(
            PublicCatalogService catalog,
            PublicReviewService reviews
    ) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute(
                "featuredBranches",
                catalog.featuredBranches()
        );

        model.addAttribute(
                "featuredPlans",
                catalog.featuredPlans()
        );

        model.addAttribute(
                "featuredClasses",
                catalog.featuredClasses()
        );

        model.addAttribute(
                "featuredReviews",
                reviews.featured()
        );

        return "guest/home";
    }
}