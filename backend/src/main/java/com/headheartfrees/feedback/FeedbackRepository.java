package com.headheartfrees.feedback;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private: nothing outside {@code feedback} may query this table.
 */
interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /**
     * The only query the public endpoint may use.
     *
     * <p>Deriving the status filter from the method name rather than accepting
     * it as a parameter is the point. A caller cannot pass
     * {@code FeedbackStatus.PENDING} to this because there is nowhere to put
     * it, so no request parameter, sort key or page argument can widen what
     * {@code /voices} returns. {@code PublicFeedbackNeverLeaksIT} asserts the
     * behaviour; this signature is what makes it hard to break in the first
     * place.
     */
    Page<Feedback> findAllByStatus(FeedbackStatus status, Pageable pageable);
}
