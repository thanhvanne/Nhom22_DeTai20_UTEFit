package com.utefit.controller.member;

import com.utefit.dto.member.ReviewForm;
import com.utefit.service.MemberReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/member/reviews")
@RequiredArgsConstructor
public class MemberReviewController {

    private final MemberReviewService reviews;

    @GetMapping
    public String list(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "reviewPage",
                reviews.myReviews(principal.getName(), page)
        );
        return "member/reviews";
    }

    @GetMapping("/new")
    public String newForm(
            Principal principal,
            Model model
    ) {
        model.addAttribute("form", new ReviewForm());
        model.addAttribute("editId", null);
        model.addAttribute("currentImage", null);
        model.addAttribute(
                "targets",
                reviews.eligibleTargets(principal.getName())
        );

        return "member/review-form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") ReviewForm form,
            BindingResult errors,
            @RequestParam(
                    value = "image",
                    required = false
            ) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!errors.hasErrors()) {
            try {
                reviews.create(
                        principal.getName(),
                        form,
                        image,
                        video
                );

                redirect.addFlashAttribute(
                        "success",
                        "Đánh giá đã được lưu."
                );

                return "redirect:/member/reviews";

            } catch (IllegalArgumentException
                     | IllegalStateException ex) {

                model.addAttribute(
                        "error",
                        ex.getMessage()
                );
            }
        }

        model.addAttribute(
                "targets",
                reviews.eligibleTargets(principal.getName())
        );
        model.addAttribute("editId", null);
        model.addAttribute("currentImage", null);

        return "member/review-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        var review = reviews.editForm(
                principal.getName(),
                id
        );

        model.addAttribute("form", review.form());
        model.addAttribute("editId", review.id());
        model.addAttribute("currentImage", review.imageUrl());
        model.addAttribute("currentVideo", review.videoUrl());
        model.addAttribute(
                "targets",
                reviews.eligibleTargets(principal.getName())
        );

        return "member/review-form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ReviewForm form,
            BindingResult errors,
            @RequestParam(
                    value = "image",
                    required = false
            ) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!errors.hasErrors()) {
            try {
                reviews.update(
                        principal.getName(),
                        id,
                        form,
                        image,
                        video
                );

                redirect.addFlashAttribute(
                        "success",
                        "Đã cập nhật đánh giá."
                );

                return "redirect:/member/reviews";

            } catch (IllegalArgumentException
                     | IllegalStateException ex) {

                model.addAttribute(
                        "error",
                        ex.getMessage()
                );
            }
        }

        // Đọc lại target thực từ DB.
        // Không tin branchId/trainerId người dùng sửa trong form.
        var original = reviews.editForm(
                principal.getName(),
                id
        );

        form.setBranchId(
                original.form().getBranchId()
        );
        form.setTrainerId(
                original.form().getTrainerId()
        );

        model.addAttribute("editId", id);
        model.addAttribute("currentVideo", original.videoUrl());
        model.addAttribute(
                "currentImage",
                original.imageUrl()
        );
        model.addAttribute(
                "targets",
                reviews.eligibleTargets(principal.getName())
        );

        return "member/review-form";
    }
}
