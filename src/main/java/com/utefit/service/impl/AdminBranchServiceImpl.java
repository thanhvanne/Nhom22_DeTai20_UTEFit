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
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager auditEntityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<Branch> getBranches(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
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
        com.utefit.validation.FormChecks.validate(dto);
        validateBranchBusinessRules(dto, null);

        String imageUrl = dto.getImageUrl();
        String imagePublicId = dto.getImagePublicId();

        // Nếu người dùng có chọn file ảnh tải lên thì gọi CloudinaryService của B
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            MediaUploadResult uploadResult = cloudinaryService.uploadImage(dto.getImageFile(), "utefit/branches");
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) try { cloudinaryService.delete(uploadResult.publicId()); } catch (RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(AdminBranchServiceImpl.class).warn("Không dọn được ảnh chi nhánh", ex); }
                }
            });
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
        com.utefit.validation.FormChecks.validate(dto);
        Branch branch = getBranchById(id);
        validateBranchBusinessRules(dto, id);

        branch.setName(dto.getName().trim());
        branch.setAddress(dto.getAddress().trim());
        branch.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        branch.setDescription(dto.getDescription());
        if (!java.util.Objects.equals(branch.getOpeningTime(), dto.getOpeningTime()) || !java.util.Objects.equals(branch.getClosingTime(), dto.getClosingTime())) {
            var today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            long groups = auditEntityManager.createQuery("select count(s) from ClassSchedule s where s.active = true and s.fitnessClass.branch.id = :id and s.scheduleDate >= :day and ((:opening is not null and s.startTime < :opening) or (:closing is not null and s.endTime > :closing))", Long.class)
                .setParameter("id", id).setParameter("day", today).setParameter("opening", dto.getOpeningTime()).setParameter("closing", dto.getClosingTime()).getSingleResult();
            long sessions = auditEntityManager.createQuery("select count(s) from PersonalTrainingSchedule s where s.active = true and s.branch.id = :id and s.scheduleDate >= :day and ((:opening is not null and s.startTime < :opening) or (:closing is not null and s.endTime > :closing))", Long.class)
                .setParameter("id", id).setParameter("day", today).setParameter("opening", dto.getOpeningTime()).setParameter("closing", dto.getClosingTime()).getSingleResult();
            if (groups > 0 || sessions > 0) throw new IllegalStateException("Giờ mới xung đột với lịch đã xếp; xử lý lịch trước khi đổi giờ mở cửa.");
        }
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
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        long commitments = auditEntityManager.createQuery("select count(m) from Membership m where m.status = com.utefit.enums.MembershipStatus.ACTIVE and m.endDate >= :today and (m.branch.id = :id or m.quotedScope = com.utefit.enums.MembershipScope.ALL_BRANCHES or :id member of m.branchIds)", Long.class).setParameter("today", today).setParameter("id", branchId).getSingleResult();
        long personal = auditEntityManager.createQuery("select count(p) from PersonalTrainingSchedule p where p.active = true and p.branch.id = :id and p.scheduleDate >= :today", Long.class).setParameter("id", branchId).setParameter("today", today).getSingleResult();
        long open = auditEntityManager.createQuery("select count(c) from Checkin c where c.branch.id = :id and c.checkOutAt is null", Long.class).setParameter("id", branchId).getSingleResult();
        if (commitments > 0 || personal > 0 || open > 0) throw new IllegalStateException("Chi nhánh còn quyền lợi đã bán, lịch PT hoặc hội viên đang tập.");
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
