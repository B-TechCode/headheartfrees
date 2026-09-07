package com.headheartfrees.feedback;

import java.util.List;
import org.springframework.data.domain.Page;
import java.util.function.Function;

/**
 * A page of results, in this project's own shape.
 *
 * <p>Spring's {@code Page} serialises to a large, unstable envelope that
 * includes the {@code Pageable} and a warning in recent versions. This is the
 * four things a client actually needs, and it does not change when Spring Data
 * does.
 */
public record FeedbackPage<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext) {

    static <E, T> FeedbackPage<T> of(Page<E> page, Function<E, T> mapper) {
        return new FeedbackPage<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
