package com.headheartfrees.feedback;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storing, listing and moderating notes.
 *
 * <h2>Why {@code createdAt} is truncated to the hour</h2>
 *
 * The brief forbids linking a feedback row to a vent row "even to someone
 * holding the database", and names timestamp correlation specifically. The
 * feedback form lives on {@code /vent/released}, so in the ordinary case a
 * submission happens within a minute or two of a release. Full-precision
 * timestamps in both tables would make that pairing obvious to anyone with a
 * dump - a release at 14:32:10 and a note at 14:33:45, at this site's traffic,
 * is one person.
 *
 * <p>Truncating to the hour makes the join useless while keeping the queue
 * roughly ordered for the moderator. It is a real cost - "submitted 3 minutes
 * ago" is not available, and two notes in the same hour have no defined order
 * between them - and it is the right trade: the ordering is a convenience, the
 * unlinkability is a promise.
 *
 * <p>Truncation happens here rather than in a database default so that it is
 * visible in the code that a reader is most likely to be auditing, and so the
 * clock is injectable and the property is testable.
 */
@Service
class DefaultFeedbackService implements FeedbackService {

    private final FeedbackRepository repository;
    private final Clock clock;

    DefaultFeedbackService(FeedbackRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest request, UUID submitterId) {
        Feedback feedback = new Feedback(
                UUID.randomUUID(),
                submitterId,
                blankToNull(request.displayName()),
                blankToNull(request.location()),
                request.rating(),
                request.message().strip(),
                Instant.now(clock).truncatedTo(ChronoUnit.HOURS));

        // The response is the caller's own submission echoed back, and it is
        // deliberately the public shape: a submitter is told what they wrote,
        // not what status it is in. "Submitted" and "published" are different
        // things and the form says which one just happened.
        return FeedbackResponse.of(repository.save(feedback));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPage<FeedbackResponse> published(Pageable pageable) {
        // The status is not a parameter. `findAllByStatus` is called with a
        // constant, so no page, size or sort argument can widen this.
        Page<Feedback> page = repository.findAllByStatus(FeedbackStatus.APPROVED, pageable);
        return FeedbackPage.of(page, FeedbackResponse::of);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPage<AdminFeedbackResponse> queue(Pageable pageable) {
        return FeedbackPage.of(repository.findAll(pageable), AdminFeedbackResponse::of);
    }

    @Override
    @Transactional
    public AdminFeedbackResponse moderate(
            UUID feedbackId, ModerateFeedbackRequest.Decision decision, UUID moderatorId) {

        Feedback feedback = repository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackId));

        // Full precision here, unlike createdAt. This records a moderator
        // acting on their own queue, not a visitor arriving, so there is
        // nothing for it to be correlated against.
        feedback.moderate(decision.toStatus(), moderatorId, Instant.now(clock));

        return AdminFeedbackResponse.of(repository.save(feedback));
    }

    /**
     * An empty optional field is absent, not empty.
     *
     * <p>The form sends {@code ""} for a name the person cleared. Storing that
     * would put a zero-length display name on a published note, which renders
     * as an anonymous entry that nonetheless occupies the space where a name
     * goes.
     */
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
