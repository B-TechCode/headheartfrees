package com.headheartfrees.vent;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link VentService}.
 *
 * <p>Logging note, which is part of rule 2.1 rather than a style preference:
 * nothing in this class logs a request body, and there is no request body worth
 * logging — the only inbound value is a six-valued enum. The debug line below
 * records the mood and nothing else. If a future change makes this service
 * accept anything wider, the logging has to be revisited at the same time.
 */
@Service
class DefaultVentService implements VentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVentService.class);

    private final VentEventRepository repository;
    private final Clock clock;

    DefaultVentService(VentEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordRelease(Mood mood) {
        repository.save(new VentEvent(mood, clock.instant()));
        // Safe to log: `mood` is an enum constant or null. Never a free string.
        log.debug("Release recorded with mood={}", mood);
    }

    @Override
    @Transactional(readOnly = true)
    public VentStatsResponse stats() {
        return new VentStatsResponse(repository.count());
    }
}
