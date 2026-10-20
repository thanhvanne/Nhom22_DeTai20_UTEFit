
package com.utefit.controller.guest;

import com.utefit.service.PublicCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PublicCatalogService catalog;

    public HomeController(PublicCatalogService catalog) {
        this.catalog = catalog;
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

        return "guest/home";
    }
}
