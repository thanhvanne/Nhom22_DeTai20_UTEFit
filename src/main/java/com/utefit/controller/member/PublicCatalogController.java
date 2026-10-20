
package com.utefit.controller.member;

import com.utefit.service.PublicCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PublicCatalogController {

    private final PublicCatalogService catalog;

    public PublicCatalogController(
            PublicCatalogService catalog
    ) {
        this.catalog = catalog;
    }

    @GetMapping("/branches")
    public String branches(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "branchPage",
                catalog.searchBranches(keyword, page)
        );
        model.addAttribute("keyword", keyword);
        return "guest/branches";
    }

    @GetMapping("/branches/{id}")
    public String branchDetail(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute(
                "branch",
                catalog.branchDetail(id)
        );
        return "guest/branch-detail";
    }

    @GetMapping("/membership-plans")
    public String plans(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "planPage",
                catalog.searchPlans(keyword, page)
        );
        model.addAttribute("keyword", keyword);
        return "guest/membership-plans";
    }

    @GetMapping("/classes")
    public String classes(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "classPage",
                catalog.searchClasses(keyword, branchId, page)
        );
        model.addAttribute(
                "branches",
                catalog.activeBranchOptions()
        );
        model.addAttribute("keyword", keyword);
        model.addAttribute("branchId", branchId);

        return "guest/classes";
    }
}
