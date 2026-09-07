package com.headheartfrees.feedback;

import com.headheartfrees.common.web.ClientIpRateLimiter;
import com.headheartfrees.common.web.RateLimitExceededException;
import com.headheartfrees.common.web.RateLimitPolicy;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public feedback endpoints: submit one, read the approved ones.
 *
 * <p>Both are reachable without an account. Submission is optional and
 * anonymous submission is allowed, so nothing on the release flow requires
 * signing in - rule 2.2 covers {@code /vent}, and this page sits directly
 * after it.
 */
@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback", description = "Submitting a note, and the approved ones.")
class FeedbackController {

    /**
     * The page size cap.
     *
     * <p>Without one, {@code ?size=100000} turns a public endpoint into a
     * full-table export served in a single request. The client asks for 12.
     */
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 12;

    private final FeedbackService feedbackService;
    private final ClientIpRateLimiter rateLimiter;

    FeedbackController(FeedbackService feedbackService, ClientIpRateLimiter rateLimiter) {
        this.feedbackService = feedbackService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Stores a note as PENDING and returns 201.
     *
     * <p>The user id comes from the security context when there is one and is
     * {@code null} otherwise. It is never read from the body: a request cannot
     * claim to be somebody, and a signed-in person is not forced to be named -
     * whether their name appears is decided by the {@code displayName} field
     * they can clear, not by whether they happen to hold a token.
     */
    @PostMapping
    @Operation(
            summary = "Submit a note",
            description = "Works signed in or anonymously. Always stored as PENDING; "
                    + "nothing appears publicly until a moderator approves it.")
    ResponseEntity<FeedbackResponse> submit(
            @Valid @RequestBody SubmitFeedbackRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        FeedbackResponse saved = feedbackService.submit(request, submitterId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * The public wall. APPROVED only, newest first.
     *
     * <p>There is no {@code status} parameter and no {@code sort} parameter.
     * Both are omissions on purpose: a caller cannot ask for pending rows and
     * cannot reorder by a column that would leak something about them.
     */
    @GetMapping
    @Operation(
            summary = "Approved notes",
            description = "Only APPROVED rows, and never the submitter, the status "
                    + "or the moderator. Paginated.")
    FeedbackPage<FeedbackResponse> published(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        return feedbackService.published(pageRequest(page, size));
    }

    /**
     * A page request built from clamped input.
     *
     * <p>A negative page or a zero size makes {@code PageRequest.of} throw
     * {@code IllegalArgumentException}, which would surface as a 500 for what
     * is really a malformed request. Clamping answers sensibly instead.
     */
    static PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * The signed-in user's id, or null.
     *
     * <p>{@code JwtAuthenticationFilter} puts the user id in the principal, so
     * this is a cast rather than a lookup. An anonymous request arrives with a
     * null authentication or Spring's {@code AnonymousAuthenticationToken},
     * and both mean the same thing here.
     */
    private UUID submitterId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UUID id ? id : null;
    }

    /** 3 per hour per caller (PROJECT_BRIEF.md section 6). */
    private void enforceRateLimit(HttpServletRequest httpRequest) {
        ConsumptionProbe probe = rateLimiter.tryConsume(httpRequest, RateLimitPolicy.FEEDBACK);
        if (!probe.isConsumed()) {
            long seconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            throw new RateLimitExceededException(Math.max(1, seconds));
        }
    }
}
