package com.utefit.controller.pt;

import com.utefit.service.PtProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pt/profile")
@RequiredArgsConstructor
public class PtProfileController {
    private final PtProfileService profiles;

    @GetMapping
    public String view(Authentication authentication, Model model) {
        model.addAttribute("trainer", profiles.myProfile(authentication.getName()));
        return "pt/profile/detail";
    }

    @GetMapping("/edit")
    public String edit(Authentication authentication, Model model) {
        model.addAttribute("trainer", profiles.myProfile(authentication.getName()));
        return "pt/profile/edit";
    }

    @PostMapping("/edit")
    public String update(Authentication authentication,
                         @RequestParam String specialization,
                         @RequestParam Integer yearsExperience,
                         @RequestParam(required = false) String bio,
                         @RequestParam(required = false) MultipartFile avatarFile,
                         RedirectAttributes redirect) {
        try {
            profiles.updateMyProfile(authentication.getName(),
                    specialization, yearsExperience, bio, avatarFile);
            redirect.addFlashAttribute("successMessage", "Cập nhật hồ sơ PT thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pt/profile";
    }
}
