
package com.utefit.service;

import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.dto.member.PasswordChangeRequest;
import com.utefit.dto.member.ProfileUpdateRequest;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MemberProfileService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public MemberProfileService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            CloudinaryService cloudinaryService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public ProfileView getProfile(String email) {
        User user = currentMember(email);

        return new ProfileView(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAvatarUrl()
        );
    }

    @Transactional(readOnly = true)
    public ProfileUpdateRequest getEditForm(String email) {
        User user = currentMember(email);

        ProfileUpdateRequest form =
                new ProfileUpdateRequest();

        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());

        return form;
    }

    @Transactional
    public void updateProfile(
            String email,
            ProfileUpdateRequest request
    ) {
        User user = currentMember(email);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());

        String phone = request.getPhone();
        user.setPhone(
                phone == null || phone.isBlank()
                        ? null
                        : phone.trim()
        );

        users.save(user);
    }

    @Transactional
    public void changePassword(
            String email,
            PasswordChangeRequest request
    ) {
        User user = currentMember(email);

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Mật khẩu hiện tại không đúng"
            );
        }

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {
            throw new IllegalArgumentException(
                    "Xác nhận mật khẩu mới không khớp"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        // Vô hiệu hóa các JWT cũ.
        int version = user.getTokenVersion() == null
                ? 0
                : user.getTokenVersion();

        user.setTokenVersion(version + 1);
        users.save(user);
    }

    @Transactional
    public void updateAvatar(
            String email,
            MultipartFile file
    ) {
        User user = currentMember(email);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ảnh đại diện"
            );
        }

        // Dùng CloudinaryService của B, không viết
        // một service upload thứ hai.
        MediaUploadResult uploaded =
                cloudinaryService.uploadImage(
                        file,
                        "utefit/member-avatars"
                );

        String oldId = user.getAvatarPublicId();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String id = status == STATUS_COMMITTED ? oldId : uploaded.publicId();
                if (id != null && !id.isBlank()) try { cloudinaryService.delete(id); } catch (RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(MemberProfileService.class).warn("Không dọn được avatar {}", id, ex); }
            }
        });
        user.setAvatarUrl(uploaded.url());
        user.setAvatarPublicId(uploaded.publicId());

        users.save(user);
    }

    private User currentMember(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản"
                        )
                );

        if (user.getRole() != UserRole.MEMBER
                || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Tài khoản không có quyền thao tác"
            );
        }

        return user;
    }

    public record ProfileView(
            Long id,
            String email,
            String firstName,
            String lastName,
            String phone,
            String avatarUrl
    ) {}
}
