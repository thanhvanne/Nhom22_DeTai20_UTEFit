package com.utefit.service.impl;

import com.utefit.dto.admin.BranchFormDto;
import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.entity.Branch;
import com.utefit.repository.BranchRepository;
import com.utefit.service.AdminBranchService;
import com.utefit.service.CloudinaryService;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.enums.MembershipStatus;
import com.utefit.enums.ClassStatus;
import java.time.LocalDate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBranchServiceImpl implements AdminBranchService {

    private final BranchRepository branchRepository;
    private final CloudinaryService cloudinaryService;
    private final MembershipRepository memberships;
    private final ClassScheduleRepository schedules;

    @Override
    @Transactional(readOnly = true)
    public Page<Branch> getBranches(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return branchRepository.searchBranches(searchPattern, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchFormDto getBranchFormById(Long id) {
        Branch b = getBranchById(id);
        return BranchFormDto.builder()
                .id(b.getId())
                .name(b.getName())
                .address(b.getAddress())
                .phone(b.getPhone())
                .description(b.getDescription())
                .imageUrl(b.getImageUrl())
                .imagePublicId(b.getImagePublicId())
                .openingTime(b.getOpeningTime())
                .closingTime(b.getClosingTime())
                .active(b.getActive())
                .build();
    }

    @Override
    @Transactional
    public void createBranch(BranchFormDto dto) {
        validateBranchBusinessRules(dto, null);

        String imageUrl = dto.getImageUrl();
        String imagePublicId = dto.getImagePublicId();

        // Nếu người dùng có chọn file ảnh tải lên thì gọi CloudinaryService của B
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            MediaUploadResult uploadResult = cloudinaryService.uploadImage(dto.getImageFile(), "utefit/branches");
            imageUrl = uploadResult.url();
            imagePublicId = uploadResult.publicId();
        }

        Branch branch = Branch.builder()
                .name(dto.getName().trim())
                .address(dto.getAddress().trim())
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : null)
                .description(dto.getDescription())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .openingTime(dto.getOpeningTime())
                .closingTime(dto.getClosingTime())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        branchRepository.save(branch);
    }

    @Override
    @Transactional
    public void updateBranch(Long id, BranchFormDto dto) {
        Branch branch = getBranchById(id);
        validateBranchBusinessRules(dto, id);

        branch.setName(dto.getName().trim());
        branch.setAddress(dto.getAddress().trim());
        branch.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        branch.setDescription(dto.getDescription());
        branch.setOpeningTime(dto.getOpeningTime());
        branch.setClosingTime(dto.getClosingTime());
        boolean nextActive = dto.getActive() != null ? dto.getActive() : true;
        if (!nextActive && Boolean.TRUE.equals(branch.getActive())) checkCanDeactivate(id);
        branch.setActive(nextActive);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String oldPublicId = branch.getImagePublicId();
            MediaUploadResult upload = cloudinaryService.uploadImage(dto.getImageFile(), "utefit/branches");
            branch.setImageUrl(upload.url());
            branch.setImagePublicId(upload.publicId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    String toDelete = status == STATUS_COMMITTED ? oldPublicId : upload.publicId();
                    if (toDelete != null && !toDelete.isBlank()) {
                        try { cloudinaryService.delete(toDelete); } catch (RuntimeException ignored) {}
                    }
                }
            });
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            branch.setImageUrl(dto.getImageUrl().trim());
        }

        branchRepository.save(branch);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Branch branch = getBranchById(id);
        boolean nextActive = !Boolean.TRUE.equals(branch.getActive());
        if (!nextActive) checkCanDeactivate(id);
        branch.setActive(nextActive);
        branchRepository.save(branch);
    }

    private void checkCanDeactivate(Long branchId) {
        LocalDate today = LocalDate.now();
        if (memberships.hasActiveMembersForBranch(branchId, MembershipStatus.ACTIVE, today)) {
            throw new IllegalStateException("Chi nhánh còn Member đã thanh toán có gói hiệu lực.");
        }
        if (schedules.hasFutureActiveSessionsAtBranch(branchId, today,
                ClassStatus.CANCELLED, ClassStatus.COMPLETED)) {
            throw new IllegalStateException("Chi nhánh còn lịch lớp tương lai; hãy hủy/chuyển lịch trước.");
        }
    }

    // Business Rule trọng tâm để chụp Source Evidence cho báo cáo
    private void validateBranchBusinessRules(BranchFormDto dto, Long currentId) {
        if (dto.getOpeningTime() != null && dto.getClosingTime() != null) {
            if (!dto.getOpeningTime().isBefore(dto.getClosingTime())) {
                throw new IllegalArgumentException("Giờ mở cửa phải nhỏ hơn giờ đóng cửa!");
            }
        }

        String name = dto.getName().trim();
        boolean duplicateName = (currentId == null)
                ? branchRepository.existsByNameIgnoreCase(name)
                : branchRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (duplicateName) {
            throw new IllegalArgumentException("Tên chi nhánh '" + name + "' đã tồn tại trong hệ thống!");
        }
    }
}
