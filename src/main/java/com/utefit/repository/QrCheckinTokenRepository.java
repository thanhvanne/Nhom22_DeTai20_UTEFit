package com.utefit.repository;

import com.utefit.entity.QrCheckinToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface QrCheckinTokenRepository extends JpaRepository<QrCheckinToken, Long> {
    Optional<QrCheckinToken> findByIdAndUser_Id(Long id, Long userId);

    @Query("SELECT t.user.id FROM QrCheckinToken t WHERE t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM QrCheckinToken t WHERE t.tokenHash = :hash")
    Optional<QrCheckinToken> lockByHash(@Param("hash") String hash);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE QrCheckinToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int revokeUnused(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
