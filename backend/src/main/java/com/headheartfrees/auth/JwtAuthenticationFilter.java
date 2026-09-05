package com.headheartfrees.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>} and populates the security context.
 *
 * <h2>Why this exists instead of {@code oauth2ResourceServer(jwt)}</h2>
 *
 * Spring's resource-server support would do the same job with less code, but it
 * installs its own {@code BearerTokenAuthenticationEntryPoint}, which answers
 * with the RFC 6750 {@code WWW-Authenticate} form rather than the single error
 * shape PROJECT_BRIEF.md section 6 mandates. A client parsing
 * {@code {timestamp, status, code, message, path}} would get something else
 * back from exactly the endpoints most likely to fail. Doing the decode here
 * means every rejection in the application still routes through
 * {@code SecurityErrorHandler}.
 *
 * <h2>What it does not do</h2>
 *
 * No database lookup. The token is self-contained and short-lived, so a request
 * costs one HMAC verification rather than one query. The cost is that a role
 * change or a deletion takes up to the access-token TTL to take effect;
 * fifteen minutes is the deliberate bound on that staleness.
 *
 * <p>An invalid or expired token is treated as <em>no</em> token rather than as
 * an error: the context is left anonymous and the filter chain decides what
 * that means for the path being requested. That is what keeps
 * {@code /api/v1/vent/**} reachable when a client sends a stale token - the
 * request is simply anonymous, and the vent endpoints are public.
 *
 * <p>Public because {@code SecurityConfig} lives in {@code config} and must
 * install it. It is module infrastructure rather than an entity or a
 * repository, so PROJECT_BRIEF.md section 4 is satisfied.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // An already-populated context means another mechanism authenticated
        // this request; do not overwrite it.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractToken(request)
                    .flatMap(jwtService::verify)
                    .flatMap(jwt -> toAuthentication(jwt, request))
                    .ifPresent(SecurityContextHolder.getContext()::setAuthentication);
        }
        chain.doFilter(request, response);
    }

    private static java.util.Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return java.util.Optional.empty();
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    /**
     * The principal is the user's {@link UUID}, not the email. Controllers that
     * need more call {@code /me}; nothing downstream should be reading identity
     * details off the token.
     */
    private static java.util.Optional<UsernamePasswordAuthenticationToken> toAuthentication(
            Jwt jwt, HttpServletRequest request) {

        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException malformed) {
            // Correctly signed but the subject is not a user id. Only reachable
            // if something else is minting tokens with our key, which is worth
            // failing closed on rather than trusting. Empty means "no token",
            // so the request continues as anonymous.
            return java.util.Optional.empty();
        }

        String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(
                new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                        .buildDetails(request));
        return java.util.Optional.of(authentication);
    }
}
