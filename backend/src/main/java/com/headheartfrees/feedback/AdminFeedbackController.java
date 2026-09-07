package com.headheartfrees.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The moderation queue.
 *
 * <h2>The first method security in this project</h2>
 *
 * {@code @PreAuthorize} appears here for the first time, closing the phase 5
 * open item: the 401-vs-403 distinction was fixed and tested through the
 * filter chain, but nothing was annotated, so the path that
 * {@code GlobalExceptionHandler} and {@code SecurityErrorHandler} actually
 * take for an {@code AccessDeniedException} raised by method security had
 * never been exercised. {@code AdminFeedbackAuthorisationIT} now pins all
 * three outcomes:
 *
 * <ul>
 *   <li>anonymous → <strong>401</strong>, because there is nobody to refuse
 *   <li>authenticated USER → <strong>403</strong>, because there is somebody
 *       and they are not allowed
 *   <li>ADMIN → <strong>200</strong>
 * </ul>
 *
 * <p>Getting that pair the wrong way round is not cosmetic. A 401 tells a
 * client "sign in again", so a signed-in user seeing one is sent to
 * re-authenticate over a permission they will never have, in a loop.
 *
 * <p>The annotation is belt as well as braces: {@code SecurityConfig} also
 * requires ADMIN for {@code /api/v1/admin/**} at the filter chain, and matches
 * that rule <em>before</em> the broader public entries so a later edit cannot
 * accidentally open this. Either mechanism alone would do; both together mean
 * a mistake in one is not a breach.
 */
@RestController
@RequestMapping("/api/v1/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Moderation. ADMIN only.")
class AdminFeedbackController {

    private final FeedbackService feedbackService;

    AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Every submission, newest first, in every status.
     *
     * <p>Sorted by {@code createdAt} like the public list. Because that column
     * is stored truncated to the hour, notes submitted in the same hour have
     * no defined order between them - a deliberate consequence of keeping
     * feedback unlinkable from releases, recorded in
     * {@code DefaultFeedbackService}.
     */
    @GetMapping
    @Operation(summary = "The queue", description = "All statuses, paginated. ADMIN only.")
    FeedbackPage<AdminFeedbackResponse> queue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return feedbackService.queue(FeedbackController.pageRequest(page, size));
    }

    /**
     * Approve or reject, from any current state.
     *
     * <p>Reversible in both directions on purpose. Community Guidelines promise
     * that someone can ask for their words to be taken down, and a queue that
     * could only move forwards would make that promise unkeepable without a
     * database console.
     */
    @PatchMapping("/{id}")
    @Operation(
            summary = "Approve or reject",
            description = "Accepts APPROVED or REJECTED from any state, so a decision "
                    + "can be reversed. Records who decided and when.")
    AdminFeedbackResponse moderate(
            @PathVariable UUID id,
            @Valid @RequestBody ModerateFeedbackRequest request,
            Authentication authentication) {

        return feedbackService.moderate(
                id, request.status(), (UUID) authentication.getPrincipal());
    }
}
