package com.utefit.controller.admin;

import com.utefit.dto.admin.ClassCategoryFormDto;
import com.utefit.entity.ClassCategory;
import com.utefit.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<ClassCategory> categoryPage = categoryService.getCategories(keyword, active, page, size);

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedActive", active);
        model.addAttribute("activeMenu", "categories");

        return "admin/categories/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryForm", new ClassCategoryFormDto());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeMenu", "categories");
        return "admin/categories/form";
    }

    @PostMapping("/create")
    public String createCategory(
            @Valid @ModelAttribute("categoryForm") ClassCategoryFormDto categoryForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "categories");
            return "admin/categories/form";
        }

        try {
            categoryService.createCategory(categoryForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo loại lớp tập mới thành công!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "categories");
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ClassCategoryFormDto categoryForm = categoryService.getCategoryFormById(id);
            model.addAttribute("categoryForm", categoryForm);
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "categories");
            return "admin/categories/form";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") ClassCategoryFormDto categoryForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        categoryForm.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "categories");
            return "admin/categories/form";
        }

        try {
            categoryService.updateCategory(id, categoryForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại lớp tập thành công!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "categories");
            return "admin/categories/form";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            categoryService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái loại lớp tập!");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}