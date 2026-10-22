package com.utefit.service;

import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PtProfileService {
    private final UserRepository users;
    private final TrainerRepository trainers;
    private final CloudinaryService cloudinary;

    @Transactional
    public Trainer myProfile(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản PT không tồn tại."));
        if (user.getRole() != UserRole.PT) {
            throw new IllegalStateException("Chỉ PT được xem hồ sơ PT.");
        }
        // Hồ sơ ban đầu không được tự chọn Branch; Admin sẽ gán.
        return trainers.findByUserId(user.getId()).orElseGet(() -> trainers.save(
                Trainer.builder().user(user).specialization("General Fitness")
                        .yearsExperience(0).bio("Chưa cập nhật.").active(false).build()));
    }

    @Transactional
    public void updateMyProfile(String email, String specialization, Integer years,
                                String bio, MultipartFile avatar) {
        Trainer trainer = myProfile(email);
        if (specialization == null || specialization.trim().length() > 255
                || years == null || years < 0 || years > 70
                || (bio != null && bio.length() > 2000)) {
            throw new IllegalArgumentException("Chuyên môn, kinh nghiệm hoặc tiểu sử không hợp lệ.");
        }
        trainer.setSpecialization(specialization.trim());
        trainer.setYearsExperience(years);
        trainer.setBio(bio == null ? null : bio.trim());
        if (avatar != null && !avatar.isEmpty()) {
            MediaUploadResult next = cloudinary.uploadImage(avatar, "avatars");
            String oldPublicId = trainer.getUser().getAvatarPublicId();
            trainer.getUser().setAvatarUrl(next.url());
            trainer.getUser().setAvatarPublicId(next.publicId());
            String deleteAfter = oldPublicId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    String id = status == STATUS_COMMITTED ? deleteAfter : next.publicId();
                    if (id != null && !id.isBlank()) {
                        try { cloudinary.delete(id); } catch (RuntimeException ignored) {}
                    }
                }
            });
        }
    }
}
