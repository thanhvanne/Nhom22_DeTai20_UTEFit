package com.utefit.repository;

import com.utefit.entity.Favorite;
import com.utefit.enums.FavoriteType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<Favorite> findByUser_IdAndTargetTypeAndTargetId(Long userId, FavoriteType type, Long targetId);
}
