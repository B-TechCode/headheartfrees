package com.headheartfrees.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a custom request header on the two endpoints authenticated purely by
 * a cookie.
 *
 * <h2>What this defends</h2>
 *
 * {@code POST /api/v1/auth/refresh} and {@code POST /api/v1/auth/logout} are
 * state-changing and carry no body a caller must know. Their only credential is
 * the {@code hhf_refresh} cookie, which the browser attaches on its own. An
 * attacker who cannot read the cookie can still cause it to be <em>sent</em>,
 * and that is enough to do damage without ever seeing a response: signing
 * someone out, or forcing a rotation that makes the next legitimate refresh
 * present a spent token and trip reuse detection, revoking the whole family. A
 * logout attack wearing the costume of a security feature firing correctly.
 *
 * <p>The defence is that a cross-site {@code <form>} post cannot set an
 * arbitrary header. Anything that can set one is a scripted request, and a
 * scripted cross-origin request with a non-safelisted header must clear a CORS
 * preflight against {@link com.headheartfrees.config.CorsProperties} first. The
 * value is a convention; the security comes from the header being present at
 * all.
 *
 * <h2>Why the cookie attribute is not enough on its own</h2>
 *
 * {@code RefreshCookie} sets {@code SameSite=Strict}, so a current browser
 * already refuses to attach it cross-site and the attack above does not
 * currently work. This is a second, independent lock, and it is worth having
 * for one specific reason: that {@code Strict} is documented as conditional.
 * The moment the frontend and backend move to different registrable domains the
 * cookie must become {@code SameSite=None; Secure}, and on that day the only
 * thing standing between a signed-in visitor and a forced logout is this
 * filter. Adding it after the migration means shipping the hole first.
 *
 * <h2>What is deliberately not guarded</h2>
 *
 * <ul>
 *   <li>{@code /register} and {@code /login} - the credential is in the body.
 *       Forging one gains an attacker nothing they could not do by simply
 *       calling the endpoint, since they would have to know the password.
 *   <li>{@code /api/v1/vent/**} - PROJECT_BRIEF.md rule 2.2. Those endpoints
 *       must work for any client with no ceremony, and there is no session to
 *       forge: nothing there is authenticated, so there is nothing to protect.
 *       {@code VentRemainsAnonymousIT} and {@code CsrfHeaderIT} both fail if
 *       this filter ever reaches them.
 *   <li>{@code /me} - a read, and already authenticated by a Bearer token that
 *       script must attach deliberately. A cookie the browser sends by itself
 *       is the whole problem; a header a caller had to set is not.
 *   <li>Every method except POST, so the CORS preflight is untouched.
 * </ul>
 *
 * <p>Not a {@code @Component}: Spring Boot auto-registers beans of type
 * {@code Filter} into the plain servlet chain, which runs <em>before</em>
 * Spring Security's, and therefore before its {@code CorsFilter}. A rejection
 * from out there would carry no CORS headers, so a browser would report an
 * opaque CORS failure instead of the section 6 body this writes. It is
 * constructed by {@code SecurityConfig} and installed in one place.
 */
public class CsrfHeaderFilter extends OncePerRequestFilter {

    /**
     * Non-safelisted on purpose. A header a cross-site form can set would prove
     * nothing; this one costs a preflight, which is exactly the check wanted.
     */
    public static final String HEADER = "X-Requested-With";

    /** The value the frontend sends. See the class note on why it is a convention. */
    public static final String REQUIRED_VALUE = "fetch";

    /**
     * Distinct from {@code UNAUTHORIZED} and from {@code FORBIDDEN} so a client
     * can tell "you forgot the header" from "your credentials are bad". They
     * need different responses: the first is a bug to fix in the caller, the
     * second means sign in again.
     */
    public static final String CODE = "CSRF_HEADER_REQUIRED";

    private static final Set<String> GUARDED_PATHS =
            Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    private final ApiErrorWriter errorWriter;

    public CsrfHeaderFilter(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (isGuarded(request) && !hasRequiredHeader(request)) {
            errorWriter.write(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    CODE,
                    "This endpoint requires the " + HEADER + ": " + REQUIRED_VALUE
                            + " header. It is set by the application's own API client.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isGuarded(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && GUARDED_PATHS.contains(normalisedPath(request));
    }

    /**
     * Trailing slashes removed so {@code /refresh/} cannot walk around the set.
     * Spring maps both to the same handler, so the guard must treat them the
     * same way or the guard is decorative.
     */
    private String normalisedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * {@code getHeader} is case-insensitive on the name per the servlet spec;
     * the value is compared case-insensitively too, because a proxy that
     * normalises case should not sign everybody out.
     */
    private boolean hasRequiredHeader(HttpServletRequest request) {
        String value = request.getHeader(HEADER);
        return value != null && REQUIRED_VALUE.equalsIgnoreCase(value.trim());
    }
}
