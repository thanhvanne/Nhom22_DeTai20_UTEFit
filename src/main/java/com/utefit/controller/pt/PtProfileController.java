package com.utefit.controller.pt;

import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import com.utefit.repository.BranchRepository;
import com.utefit.service.CloudinaryService;
import com.utefit.dto.cloudinary.MediaUploadResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pt/profile")
public class PtProfileController {

    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final CloudinaryService cloudinaryService;

    public PtProfileController(TrainerRepository trainerRepository,
                               UserRepository userRepository,
                               BranchRepository branchRepository,
                               CloudinaryService cloudinaryService) {
        this.trainerRepository = trainerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.cloudinaryService = cloudinaryService;
    }

    private Trainer getCurrentTrainer(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản đăng nhập!"));

        return trainerRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Trainer newTrainer = Trainer.builder()
                            .user(user)
                            .specialization("General Fitness")
                            .yearsExperience(1)
                            .bio("Chưa cập nhật tiểu sử.")
                            .active(true)
                            .build();
                    return trainerRepository.save(newTrainer);
                });
    }

    // /pt/profile
    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        Trainer trainer = getCurrentTrainer(authentication);
        model.addAttribute("trainer", trainer);
        return "pt/profile/detail";
    }

    // /pt/profile/edit
    @GetMapping("/edit")
    public String editForm(Authentication authentication, Model model) {
        Trainer trainer = getCurrentTrainer(authentication);
        model.addAttribute("trainer", trainer);
        model.addAttribute("branches", branchRepository.findAll());
        return "pt/profile/edit";  // đúng với path bạn nói
    }

    @PostMapping("/edit")
    public String updateProfile(
            Authentication authentication,
            @RequestParam("specialization") String specialization,
            @RequestParam("yearsExperience") Integer yearsExperience,
            @RequestParam("bio") String bio,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Trainer trainer = getCurrentTrainer(authentication);

            trainer.setSpecialization(specialization);
            trainer.setYearsExperience(yearsExperience);
            trainer.setBio(bio);

            if (branchId != null) {
                branchRepository.findById(branchId).ifPresent(trainer::setBranch);
            }

            if (avatarFile != null && !avatarFile.isEmpty()) {
                MediaUploadResult uploadResult = cloudinaryService.uploadImage(avatarFile, "avatars");

                if (uploadResult != null) {
                    String url = uploadResult.url();
                    String publicId = uploadResult.publicId();

                    trainer.getUser().setAvatarUrl(url);
                    trainer.getUser().setAvatarPublicId(publicId);
                    userRepository.save(trainer.getUser());
                }
            }

            trainerRepository.save(trainer);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/pt/profile";
    }
}