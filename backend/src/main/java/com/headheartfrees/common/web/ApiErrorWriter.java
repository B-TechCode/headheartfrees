package com.headheartfrees.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes an {@link ApiErrorResponse} straight to the servlet response.
 *
 * <p>For the rejections that never reach {@code @RestControllerAdvice} because
 * they happen in the filter chain, before the {@code DispatcherServlet} runs.
 * {@link SecurityErrorHandler} was the only such case until
 * {@link CsrfHeaderFilter} became the second, and at two call sites the
 * serialisation belongs in one place: the section 6 shape is a contract with
 * every client, and three hand-rolled copies of it drift.
 *
 * <p>Deliberately not a general error helper. Anything that can reach a
 * controller should throw, and {@link GlobalExceptionHandler} should render it.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param code the stable machine-readable identifier clients switch on.
     *        Distinct codes for distinct causes: a client that cannot tell a
     *        missing header from a bad credential retries the wrong thing.
     */
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message)
            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(status.value(), code, message, request.getRequestURI()));
    }
}
