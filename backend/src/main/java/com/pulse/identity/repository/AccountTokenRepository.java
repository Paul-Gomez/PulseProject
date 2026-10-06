package com.pulse.identity.repository;

import com.pulse.identity.entity.AccountToken;
import com.pulse.identity.entity.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findByTokenHashAndType(String tokenHash, AccountTokenType type);

    @Modifying
    @Query("DELETE FROM AccountToken t WHERE t.userId = :userId AND t.type = :type AND t.usedAt IS NULL")
    void deleteUnused(UUID userId, AccountTokenType type);

    /** Only one caller can flip usedAt from null, so a token cannot be spent twice even by simultaneous requests. */
    @Modifying
    @Query("UPDATE AccountToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL")
    int markUsed(UUID id, Instant now);
}
