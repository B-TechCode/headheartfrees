package com.headheartfrees.vent;

import com.headheartfrees.common.web.ClientIpRateLimiter;
import com.headheartfrees.common.web.RateLimitExceededException;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The vent endpoints. Both are public — PROJECT_BRIEF.md rule 2.2: venting
 * requires no account, and nothing added here may change that.
 *
 * <p><strong>Neither method accepts text.</strong> {@link ReleaseRequest} has a
 * single field of type {@link Mood}. There is no overload, no query parameter
 * and no header that carries what the person wrote, because it is never sent.
 */
@RestController
@RequestMapping("/api/v1/vent")
@Tag(name = "Vent", description = "Anonymous release counter. Never accepts vent text.")
class VentController {

    private final VentService ventService;
    private final ClientIpRateLimiter rateLimiter;

    VentController(VentService ventService, ClientIpRateLimiter rateLimiter) {
        this.ventService = ventService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Records a release.
     *
     * <p>Returns 204: there is nothing to give back. The client has already
     * cleared its textarea and moved on by the time this resolves, and it does
     * not wait for the answer.
     *
     * <p>A null or absent body is accepted and treated as "no mood", so a client
     * that posts nothing at all still counts.
     */
    @PostMapping("/release")
    @Operation(
            summary = "Record one release",
            description = "Accepts an optional mood label and nothing else. "
                    + "Vent text is never transmitted and this endpoint cannot receive it.")
    ResponseEntity<Void> release(
            @Valid @RequestBody(required = false) ReleaseRequest request,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        ventService.recordRelease(request == null ? null : request.mood());
        return ResponseEntity.noContent().build();
    }

    /** The real release count. Public, uncached, unpadded. */
    @GetMapping("/stats")
    @Operation(summary = "Total releases", description = "The real count. Not seeded or rounded.")
    VentStatsResponse stats() {
        return ventService.stats();
    }

    /**
     * 30 per minute per caller (section 6).
     *
     * <p>The probe's nanosecond figure is rounded up, so a caller is never told
     * to retry sooner than a token actually exists.
     */
    private void enforceRateLimit(HttpServletRequest httpRequest) {
        ConsumptionProbe probe = rateLimiter.tryConsume(httpRequest);
        if (!probe.isConsumed()) {
            long seconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            throw new RateLimitExceededException(Math.max(1, seconds));
        }
    }
}
