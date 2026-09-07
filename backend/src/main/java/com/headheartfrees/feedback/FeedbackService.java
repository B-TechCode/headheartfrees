package com.headheartfrees.feedback;

import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * The feedback module's only public surface (PROJECT_BRIEF.md section 4).
 *
 * <p>Note what the submission method takes: a request and an optional user id.
 * It does not take an {@code Authentication}, a principal, or anything else
 * from Spring Security, so this module has no opinion about how someone was
 * identified and no dependency on the code that identified them.
 */
public interface FeedbackService {

    /**
     * Stores a note as PENDING.
     *
     * @param submitterId the signed-in user's id, or {@code null} for an
     *        anonymous submission. Anonymous is a first-class case: rule 2.2
     *        makes the whole flow account-free, and someone who happens to be
     *        signed in must not be attributed against their will.
     */
    FeedbackResponse submit(SubmitFeedbackRequest request, UUID submitterId);

    /** Approved notes only, newest first. The public wall. */
    FeedbackPage<FeedbackResponse> published(Pageable pageable);

    /** Every submission in every status. ADMIN only. */
    FeedbackPage<AdminFeedbackResponse> queue(Pageable pageable);

    /** Records a decision and returns the updated item. ADMIN only. */
    AdminFeedbackResponse moderate(
            UUID feedbackId, ModerateFeedbackRequest.Decision decision, UUID moderatorId);
}
