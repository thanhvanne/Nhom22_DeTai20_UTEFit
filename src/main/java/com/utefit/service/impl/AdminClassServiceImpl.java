package com.utefit.service.impl;

import com.utefit.dto.admin.FitnessClassFormDto;
import com.utefit.entity.*;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.NotificationType;
import com.utefit.enums.RegistrationStatus;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.*;
import com.utefit.service.AdminClassService;
import com.utefit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminClassServiceImpl implements AdminClassService {

    private final FitnessClassRepository fitnessClassRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final BranchRepository branchRepository;
    private final ClassCategoryRepository categoryRepository;
    private final TrainerRepository trainerRepository;

    // GD5-C: Repository de tim Member da dang ky.
    private final ClassRegistrationRepository classRegistrationRepository;

    // GD5-C: Su dung Service Notification cua thanh vien B.
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<FitnessClass> getClasses(
            String keyword,
            Long branchId,
            Long categoryId,
            Long trainerId,
            ClassStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        String searchPattern =
                keyword != null && !keyword.trim().isEmpty()
                        ? "%" + keyword.trim().toLowerCase() + "%"
                        : null;

        return fitnessClassRepository.searchAdminClasses(
                searchPattern,
                branchId,
                categoryId,
                trainerId,
                status,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FitnessClass getClassById(Long id) {
        return fitnessClassRepository.findDetailById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy lớp tập với ID: " + id
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public FitnessClassFormDto getClassFormById(Long id) {
        FitnessClass fc = getClassById(id);

        return FitnessClassFormDto.builder()
                .id(fc.getId())
                .name(fc.getName())
                .description(fc.getDescription())
                .branchId(fc.getBranch() != null
                        ? fc.getBranch().getId() : null)
                .categoryId(fc.getCategory() != null
                        ? fc.getCategory().getId() : null)
                .trainerId(fc.getTrainer() != null
                        ? fc.getTrainer().getId() : null)
                .capacity(fc.getCapacity())
                .status(fc.getStatus())
                .build();
    }

    @Override
    @Transactional
    public void createClass(FitnessClassFormDto dto) {
        validateCapacity(dto.getCapacity());

        Branch branch = requireActiveBranch(dto.getBranchId());
        ClassCategory category =
                requireActiveCategory(dto.getCategoryId());

        Trainer trainer = resolveAndValidateTrainer(
                dto.getTrainerId(),
                branch,
                category
        );

        FitnessClass fitnessClass = FitnessClass.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription() != null
                        ? dto.getDescription().trim() : null)
                .branch(branch)
                .category(category)
                .trainer(trainer)
                .capacity(dto.getCapacity())
                .status(dto.getStatus() != null
                        ? dto.getStatus() : ClassStatus.OPEN)
                .build();

        fitnessClassRepository.save(fitnessClass);
    }

    @Override
    @Transactional
    public void updateClass(Long id, FitnessClassFormDto dto) {
        FitnessClass fitnessClass = getClassById(id);

        validateCapacity(dto.getCapacity());

        Branch branch = requireActiveBranch(dto.getBranchId());
        ClassCategory category =
                requireActiveCategory(dto.getCategoryId());

        Trainer trainer = resolveAndValidateTrainer(
                dto.getTrainerId(),
                branch,
                category
        );

        // GD5-C: Luu trang thai truoc khi cap nhat.
        ClassStatus previousStatus = fitnessClass.getStatus();

        ClassStatus targetStatus = dto.getStatus() != null
                ? dto.getStatus()
                : previousStatus;

        fitnessClass.setName(dto.getName().trim());
        fitnessClass.setDescription(
                dto.getDescription() != null
                        ? dto.getDescription().trim() : null
        );
        fitnessClass.setBranch(branch);
        fitnessClass.setCategory(category);
        fitnessClass.setTrainer(trainer);
        fitnessClass.setCapacity(dto.getCapacity());
        fitnessClass.setStatus(targetStatus);

        fitnessClassRepository.save(fitnessClass);

        // GD5-C: Neu sua trang thai thanh CANCELLED.
        if (targetStatus == ClassStatus.CANCELLED) {

            deactivateSchedulesOfClass(id);

            // Chi gui khi trang thai moi chuyen sang CANCELLED.
            if (previousStatus != ClassStatus.CANCELLED) {
                notifyMembersOfCancelledClass(fitnessClass);
            }
        }
    }

    @Override
    @Transactional
    public void cancelClass(Long id) {
        FitnessClass fitnessClass = getClassById(id);

        // Khong cho huy mot lop da huy.
        if (fitnessClass.getStatus() == ClassStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Lớp tập này đã ở trạng thái HỦY trước đó!"
            );
        }

        // 1. Chuyen trang thai lop.
        fitnessClass.setStatus(ClassStatus.CANCELLED);
        fitnessClassRepository.save(fitnessClass);

        // 2. Dong cac buoi hoc thuoc lop.
        deactivateSchedulesOfClass(id);

        // 3. Gui thong bao cho Member.
        notifyMembersOfCancelledClass(fitnessClass);
    }

    // =====================================================
    // GD5-C: GUI THONG BAO KHI HUY TOAN BO LOP
    // =====================================================
    private void notifyMembersOfCancelledClass(
            FitnessClass fitnessClass
    ) {
        // Lay danh sach Member dang REGISTERED.
        List<Long> memberIds =
                classRegistrationRepository
                        .findDistinctUserIdsByClassAndStatus(
                                fitnessClass.getId(),
                                RegistrationStatus.REGISTERED
                        );

        String title = "Lớp tập đã bị hủy";

        String content =
                "Lớp '" + fitnessClass.getName()
                        + "' đã được quản trị viên hủy. "
                        + "Các lịch học thuộc lớp này sẽ không tiếp tục diễn ra. "
                        + "Vui lòng xem lại lịch tập của bạn.";

        // Goi lai NotificationService cua B.
        for (Long memberId : memberIds) {

            notificationService.createNotificationForUser(
                    memberId,
                    NotificationType.CLASS_SCHEDULE_CANCELLED,
                    title,
                    content,
                    fitnessClass.getId()
            );
        }
    }

    // Phuong thuc cu: Dong tat ca cac lich dang ACTIVE.
    private void deactivateSchedulesOfClass(Long classId) {
        List<ClassSchedule> activeSchedules =
                classScheduleRepository
                        .findByFitnessClassIdAndActiveTrue(classId);

        for (ClassSchedule schedule : activeSchedules) {
            schedule.setActive(false);
        }

        if (!activeSchedules.isEmpty()) {
            classScheduleRepository.saveAll(activeSchedules);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> getActiveBranches() {
        return branchRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .filter(b -> Boolean.TRUE.equals(b.getActive()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassCategory> getActiveCategories() {
        return categoryRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> getActiveTrainers() {
        return trainerRepository.searchAdminTrainers(
                        null,
                        null,
                        true,
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .filter(t ->
                        Boolean.TRUE.equals(t.getActive())
                                && t.getUser() != null
                                && Boolean.TRUE.equals(
                                t.getUser().getEnabled())
                                && t.getUser().getStatus()
                                == UserStatus.ACTIVE
                                && t.getUser().getRole()
                                == UserRole.PT
                )
                .toList();
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException(
                    "Sức chứa của lớp tập (Capacity) phải lớn hơn 0!"
            );
        }
    }

    private Branch requireActiveBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy chi nhánh với ID: "
                                        + branchId
                        ));

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new IllegalArgumentException(
                    "Chi nhánh '" + branch.getName()
                            + "' đang tạm ngưng hoạt động, "
                            + "không thể gán lớp tập!"
            );
        }

        return branch;
    }

    private ClassCategory requireActiveCategory(Long categoryId) {
        ClassCategory category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy loại lớp với ID: "
                                                + categoryId
                                ));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException(
                    "Loại lớp '" + category.getName()
                            + "' đang tạm ngưng hoạt động!"
            );
        }

        return category;
    }

    private Trainer resolveAndValidateTrainer(
            Long trainerId,
            Branch classBranch,
            ClassCategory category
    ) {
        if (trainerId == null) {
            return null;
        }

        Trainer trainer =
                trainerRepository.findAdminTrainerById(trainerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy huấn luyện viên với ID: "
                                                + trainerId
                                ));

        if (!Boolean.TRUE.equals(trainer.getActive())
                || trainer.getUser() == null
                || !Boolean.TRUE.equals(
                trainer.getUser().getEnabled())
                || trainer.getUser().getStatus()
                != UserStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Huấn luyện viên được chọn hiện "
                            + "không ở trạng thái ACTIVE!"
            );
        }

        if (trainer.getBranch() != null
                && !trainer.getBranch().getId()
                .equals(classBranch.getId())) {

            throw new IllegalArgumentException(
                    "PT '"
                            + trainer.getUser().getFirstName()
                            + " "
                            + trainer.getUser().getLastName()
                            + "' đang thuộc chi nhánh '"
                            + trainer.getBranch().getName()
                            + "', không khớp với chi nhánh '"
                            + classBranch.getName()
                            + "' của lớp tập!"
            );
        }

        String spec = trainer.getSpecialization() != null
                ? trainer.getSpecialization()
                  .trim().toLowerCase()
                : "";

        String catName = category.getName() != null
                ? category.getName().trim().toLowerCase()
                : "";

        if (!spec.isEmpty()
                && !spec.contains("general")
                && !spec.contains("đa năng")
                && !spec.contains(catName)
                && !catName.contains(spec)) {

            throw new IllegalArgumentException(
                    "Chuyên môn của PT ("
                            + trainer.getSpecialization()
                            + ") không phù hợp để dạy loại lớp '"
                            + category.getName()
                            + "'!"
            );
        }

        return trainer;
    }
}
