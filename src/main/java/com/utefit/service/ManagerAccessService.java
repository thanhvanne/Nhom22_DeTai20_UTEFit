package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerAccessService {
    private final UserRepository users;
    private final BranchRepository branches;
    private final ManagerBranchAssignmentRepository assignments;
    private final PasswordEncoder encoder;

    public record ManagerRow(Long id, String email, String name, List<Branch> branches) {}

    public List<ManagerRow> managers(String adminEmail) {
        account(adminEmail, UserRole.ADMIN, false);
        return users.searchUsers(null, UserRole.MANAGER, null, PageRequest.of(0, 100)).stream()
                .map(manager -> new ManagerRow(manager.getId(), manager.getEmail(), manager.getFullName(),
                        assigned(manager.getId()))).toList();
    }

    public List<Branch> availableBranches(String adminEmail) {
        account(adminEmail, UserRole.ADMIN, false);
        return branches.findByActiveTrueOrderByNameAsc();
    }

    public List<Branch> myBranches(String email) {
        return assigned(account(email, UserRole.MANAGER, false).getId());
    }

    public Branch requireBranch(String email, Long branchId, boolean lock) {
        User manager = account(email, UserRole.MANAGER, lock);
        if (branchId == null) {
            throw new AccessDeniedException("Chưa chọn chi nhánh được phân công.");
        }
        return assignments.findByManager_IdAndBranch_Id(manager.getId(), branchId)
                .orElseThrow(() -> new AccessDeniedException("Chi nhánh không thuộc phạm vi quản lý của bạn."))
                .getBranch();
    }

    @Transactional
    public Long createManager(String adminEmail, String email, String name, String password) {
        account(adminEmail, UserRole.ADMIN, false);
        com.utefit.validation.InputRules.require("email",email);
        com.utefit.validation.InputRules.require("managerPassword",password);
        String nameError = com.utefit.validation.InputRules.error("personalName",name);
        if (nameError != null || name == null || name.isBlank()) throw new com.utefit.validation.InputViolationException(Map.of("name",nameError == null ? "Vui lòng nhập tên Manager." : nameError));
        name = com.utefit.validation.InputRules.normalize(name,com.utefit.validation.InputRules.rule("personalName"));
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100 || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || name == null || name.isBlank() || name.trim().length() > 50
                || password == null || password.length() < 12 || password.length() > 72) {
            throw new IllegalArgumentException("Email/tên không hợp lệ; mật khẩu mới cần 12–72 ký tự.");
        }
        if (users.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email đã tồn tại. Không chuyển role tài khoản đang sử dụng.");
        }
        return users.saveAndFlush(User.builder().email(normalized).firstName(name.trim()).lastName("")
                .password(encoder.encode(password)).role(UserRole.MANAGER).status(UserStatus.ACTIVE)
                .enabled(true).tokenVersion(0).build()).getId();
    }

    @Transactional
    public void grant(String adminEmail, Long managerId, Long branchId) {
        account(adminEmail, UserRole.ADMIN, false);
        User manager = manager(managerId);
        Branch branch = branches.findById(branchId).filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không hoạt động hoặc không tồn tại."));
        if (assignments.findByManager_IdAndBranch_Id(managerId, branchId).isEmpty()) {
            assignments.saveAndFlush(ManagerBranchAssignment.builder().manager(manager).branch(branch).build());
        }
    }

    @Transactional
    public void revoke(String adminEmail, Long managerId, Long branchId) {
        account(adminEmail, UserRole.ADMIN, false);
        manager(managerId);
        assignments.findByManager_IdAndBranch_Id(managerId, branchId).ifPresent(assignments::delete);
    }

    private User manager(Long id) {
        return users.lockByIdForBooking(id).filter(user -> user.getRole() == UserRole.MANAGER)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Manager."));
    }

    private List<Branch> assigned(Long managerId) {
        return assignments.findByManager_IdOrderByBranch_NameAsc(managerId).stream()
                .map(ManagerBranchAssignment::getBranch).toList();
    }

    private User account(String email, UserRole role, boolean lock) {
        User user = (lock ? users.lockByEmailForBooking(email) : users.findByEmail(email))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        if (user.getRole() != role || user.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccessDeniedException("Tài khoản không có quyền thực hiện thao tác này.");
        }
        return user;
    }
}
