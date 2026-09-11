package com.headheartfrees.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package-private, per PROJECT_BRIEF.md section 4. */
interface UserTotpRepository extends JpaRepository<UserTotp, UUID> {

    Optional<UserTotp> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
