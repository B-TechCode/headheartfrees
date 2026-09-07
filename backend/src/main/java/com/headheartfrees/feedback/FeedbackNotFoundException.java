package com.headheartfrees.feedback;

import java.util.UUID;

/** Moderating an id that is not there. Rendered as a 404. */
public class FeedbackNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FeedbackNotFoundException(UUID id) {
        super("No feedback with id " + id + ".");
    }
}
