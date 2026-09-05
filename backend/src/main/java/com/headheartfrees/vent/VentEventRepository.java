package com.headheartfrees.vent;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private, per the module boundary rule in PROJECT_BRIEF.md section 4.
 * Other domains reach the vent module through {@link VentService} only.
 */
interface VentEventRepository extends JpaRepository<VentEvent, Long> {
}
