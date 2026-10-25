package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {
    private final FavoriteRepository favorites;
    private final UserRepository users;
    private final TrainerRepository trainers;
    private final FitnessClassRepository classes;
    private final BranchRepository branches;

    public record Item(Long id, FavoriteType type, Long targetId, String name, String url, boolean available) {}

    public Map<String, Long> saved(String email) {
        User member = member(email, false);
        Map<String, Long> result = new HashMap<>();
        favorites.findByUser_IdOrderByCreatedAtDesc(member.getId()).forEach(favorite ->
                result.put(favorite.getTargetType() + ":" + favorite.getTargetId(), favorite.getId()));
        return result;
    }

    public List<Item> list(String email) {
        User member = member(email, false);
        return favorites.findByUser_IdOrderByCreatedAtDesc(member.getId()).stream().map(this::item).toList();
    }

    @Transactional
    public Long add(String email, FavoriteType type, Long targetId) {
        User member = member(email, true);
        if (type == null || targetId == null || targetId <= 0) {
            throw new IllegalArgumentException("Mục yêu thích không hợp lệ.");
        }
        Item target = item(Favorite.builder().targetType(type).targetId(targetId).build());
        if (!target.available()) {
            throw new IllegalArgumentException("Mục này không còn hoạt động hoặc không tồn tại.");
        }
        return favorites.findByUser_IdAndTargetTypeAndTargetId(member.getId(), type, targetId)
                .orElseGet(() -> favorites.saveAndFlush(Favorite.builder().user(member)
                        .targetType(type).targetId(targetId).build())).getId();
    }

    @Transactional
    public void remove(String email, Long id) {
        User member = member(email, true);
        Favorite favorite = favorites.findById(id).orElse(null);
        if (favorite == null) {
            return;
        }
        if (!favorite.getUser().getId().equals(member.getId())) {
            throw new AccessDeniedException("Không được thay đổi yêu thích của Member khác.");
        }
        favorites.delete(favorite);
    }

    private User member(String email, boolean lock) {
        User member = (lock ? users.lockByEmailForBooking(email) : users.findByEmail(email))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản."));
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new AccessDeniedException("Chỉ Member đang hoạt động được sử dụng yêu thích.");
        }
        return member;
    }

    private Item item(Favorite favorite) {
        Long targetId = favorite.getTargetId();
        return switch (favorite.getTargetType()) {
            case BRANCH -> branches.findById(targetId).map(branch -> new Item(favorite.getId(),
                    FavoriteType.BRANCH, targetId, branch.getName(), "/branches/" + targetId,
                    Boolean.TRUE.equals(branch.getActive()))).orElseGet(() -> missing(favorite));
            case TRAINER -> trainers.findById(targetId).map(trainer -> new Item(favorite.getId(),
                    FavoriteType.TRAINER, targetId, trainer.getUser().getFullName(), "/trainers/" + targetId,
                    Boolean.TRUE.equals(trainer.getActive()) && trainer.getUser().getRole() == UserRole.PT
                            && trainer.getUser().getStatus() == UserStatus.ACTIVE
                            && Boolean.TRUE.equals(trainer.getUser().getEnabled())
                            && trainer.getBranch() != null && Boolean.TRUE.equals(trainer.getBranch().getActive())))
                    .orElseGet(() -> missing(favorite));
            case CLASS -> classes.findById(targetId).map(fitnessClass -> new Item(favorite.getId(),
                    FavoriteType.CLASS, targetId, fitnessClass.getName(), "/classes?branchId=" + fitnessClass.getBranch().getId(),
                    (fitnessClass.getStatus() == ClassStatus.OPEN || fitnessClass.getStatus() == ClassStatus.FULL)
                            && Boolean.TRUE.equals(fitnessClass.getBranch().getActive())))
                    .orElseGet(() -> missing(favorite));
        };
    }

    private Item missing(Favorite favorite) {
        return new Item(favorite.getId(), favorite.getTargetType(), favorite.getTargetId(),
                "Mục không còn tồn tại (#" + favorite.getTargetId() + ")", null, false);
    }
}
