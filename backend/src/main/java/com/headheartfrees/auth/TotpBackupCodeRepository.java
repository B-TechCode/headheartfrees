package com.headheartfrees.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package-private, per PROJECT_BRIEF.md section 4. */
interface TotpBackupCodeRepository extends JpaRepository<TotpBackupCode, UUID> {

    List<TotpBackupCode> findByUserId(UUID userId);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByUserId(UUID userId);
}
