package com.headheartfrees.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Renders filter-chain security rejections in the same shape as every other
 * error (PROJECT_BRIEF.md section 6).
 *
 * <p>This exists because {@link GlobalExceptionHandler} cannot cover these
 * cases. A request refused by Spring Security is refused before the
 * {@code DispatcherServlet} runs, so no {@code @RestControllerAdvice} ever sees
 * it. Without this class an unauthenticated call returns Spring's default error
 * body and a client parsing the documented shape gets nothing usable - which is
 * the common path today, since the filter chain currently requires
 * authentication for everything outside the public path list.
 *
 * <p>Both interfaces are implemented by one class so the two responses cannot
 * drift apart.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApiErrorWriter errorWriter;

    SecurityErrorHandler(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    /** No credentials, or credentials that did not authenticate. */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication is required to access this resource.");
    }

    /** Authenticated, but not permitted. */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {

        write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource.");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message)
            throws IOException {

        // The exception itself is not logged: an unauthenticated request to a
        // protected path is normal traffic, not a fault.
        //
        // Serialisation moved to ApiErrorWriter when CsrfHeaderFilter became a
        // second filter-chain rejection needing the same shape. Behaviour here
        // is unchanged; the copy is not.
        errorWriter.write(request, response, status, code, message);
    }
}
