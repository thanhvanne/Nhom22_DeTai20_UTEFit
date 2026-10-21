
package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCheckinService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final MembershipRepository membershipRepository;
    private final CheckinRepository checkinRepository;
    private final EntityManager entityManager;

    @Transactional
    public void checkIn(String email, Long branchId) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập email Member.");
        }

        if (branchId == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn chi nhánh.");
        }

        User user = userRepository
                .findByEmail(email.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy Member với email này."));

        // Khóa theo User để tránh hai lượt check-in đồng thời.
        user = entityManager.find(
                User.class, user.getId(),
                LockModeType.PESSIMISTIC_WRITE);

        if (user.getRole() != UserRole.MEMBER
                || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Tài khoản Member không hoạt động hoặc không hợp lệ.");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy chi nhánh."));

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new IllegalArgumentException(
                    "Chi nhánh đang ngừng hoạt động.");
        }

        LocalDate today = LocalDate.now();

        List<Membership> memberships =
                membershipRepository
                        .findByUser_IdOrderByCreatedAtDesc(user.getId());

        boolean eligible = memberships.stream().anyMatch(m ->
                m.getStatus() == MembershipStatus.ACTIVE
                        && m.getStartDate() != null
                        && m.getEndDate() != null
                        && !m.getStartDate().isAfter(today)
                        && !m.getEndDate().isBefore(today)
                        && m.getBranch().getId().equals(branchId)
        );

        if (!eligible) {
            throw new IllegalArgumentException(
                    "Member chưa có Membership ACTIVE, còn hạn "
                            + "tại chi nhánh được chọn.");
        }

        if (checkinRepository
                .existsByUser_IdAndCheckOutAtIsNull(user.getId())) {
            throw new IllegalArgumentException(
                    "Member đang có một lượt check-in chưa checkout.");
        }

        Checkin checkin = Checkin.builder()
                .user(user)
                .branch(branch)
                .checkInAt(LocalDateTime.now())
                .build();

        checkinRepository.save(checkin);
    }

    @Transactional
    public void checkOut(Long checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy lượt check-in."));

        // Khóa User rồi đọc lại bản ghi để tránh checkout trùng.
        entityManager.find(
                User.class,
                checkin.getUser().getId(),
                LockModeType.PESSIMISTIC_WRITE);

        entityManager.refresh(checkin);

        if (!checkin.isOpen()) {
            throw new IllegalArgumentException(
                    "Lượt này đã checkout trước đó.");
        }

        checkin.setCheckOutAt(LocalDateTime.now());
        checkinRepository.save(checkin);
    }

    @Transactional(readOnly = true)
    public Page<Checkin> history(
            String email,
            Long branchId,
            LocalDate date,
            Boolean open,
            int page
    ) {
        String pattern = email == null || email.isBlank()
                ? null
                : "%" + email.trim().toLowerCase() + "%";

        LocalDateTime start = date == null
                ? null : date.atStartOfDay();

        LocalDateTime end = date == null
                ? null : date.plusDays(1).atStartOfDay();

        return checkinRepository.search(
                branchId,
                pattern,
                start,
                end,
                open,
                PageRequest.of(
                        Math.max(page, 0),
                        10,
                        Sort.by(Sort.Direction.DESC, "checkInAt")
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Branch> branches() {
        return branchRepository.findByActiveTrueOrderByNameAsc();
    }
}
