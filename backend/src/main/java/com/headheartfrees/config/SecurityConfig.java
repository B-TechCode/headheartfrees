package com.headheartfrees.config;

import com.headheartfrees.auth.JwtAuthenticationFilter;
import com.headheartfrees.common.web.ApiErrorWriter;
import com.headheartfrees.common.web.CsrfHeaderFilter;
import com.headheartfrees.common.web.SecurityErrorHandler;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security for a stateless JSON API.
 *
 * <p>Phase 1 scope: no authentication mechanism exists yet, so this chain only
 * establishes the shape - sessions off, CSRF off (there is no session cookie to
 * protect and no browser form posts), CORS driven by {@link CorsProperties}, and
 * an explicit list of public paths. Login, JWT filtering and role rules arrive in
 * phase 4; the full header and CORS lockdown in phase 9.
 */
@Configuration
@EnableWebSecurity
/*
 * Method security is on from phase 7, for AdminFeedbackController's
 * @PreAuthorize. It is a second, independent check rather than a replacement
 * for the path rule above: the annotation guards the methods even if a future
 * refactor moves or renames the URL, and the path rule guards the URL even if
 * somebody removes the annotation.
 */
@EnableMethodSecurity
public class SecurityConfig {

    /** Paths that must stay reachable without an account. */
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/health",
        // Venting never requires an account (PROJECT_BRIEF.md section 2.2).
        // Phase 5 introduced authentication and did not touch this line, which
        // is the point: VentRemainsAnonymousIT fails the build if a credential
        // ever becomes necessary here.
        "/api/v1/vent/**",
        // Register, login and refresh must be reachable by someone with no
        // token. /me is deliberately absent - it is the one auth endpoint that
        // requires authentication.
        // Submitting a note and reading the approved wall. Both public:
        // submission is optional and anonymous submission is allowed, so the
        // release flow never requires an account. NOTE these are the exact
        // paths, not /api/v1/feedback/** - the admin queue lives under
        // /api/v1/admin/feedback and must not be caught by a wildcard here.
        "/api/v1/feedback",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        // The second half of a sign-in, and the enrolment an admin is sent to
        // instead of a session. Public because the caller legitimately has no
        // session yet - that is what a challenge is.
        //
        // NOT unauthenticated. Each demands a ticket that is issued only after
        // a correct password, lives five minutes, and is refused by
        // JwtAuthenticationFilter if presented as a Bearer token
        // (TotpTicketIsNotAnAccessTokenIT). /totp/backup-codes and
        // /totp/disable are deliberately absent: they act on an account that
        // is already signed in, so they fall to anyRequest().authenticated().
        "/api/v1/auth/login/totp",
        "/api/v1/auth/totp/setup",
        "/api/v1/auth/totp/enable",
        // Google sign-in entry and callback.
        "/oauth2/**",
        "/login/oauth2/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
    };

    /**
     * @param clientRegistrations an {@link ObjectProvider} rather than a direct
     *        dependency, because Spring only creates a
     *        {@code ClientRegistrationRepository} when the Google properties are
     *        actually set. Injecting it directly would make a missing
     *        {@code GOOGLE_CLIENT_ID} a startup failure and take the whole
     *        application down - including the password login that has nothing
     *        to do with Google. {@code OAuth2AbsentConfigIT} asserts the context
     *        starts with those properties absent.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorHandler securityErrorHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiErrorWriter apiErrorWriter,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            ObjectProvider<AuthenticationSuccessHandler> oauth2SuccessHandler)
            throws Exception {
        // withDefaults() picks up the bean named `corsConfigurationSource` below.
        // Injecting CorsConfigurationSource directly is ambiguous: Spring MVC's
        // HandlerMappingIntrospector also implements that interface.
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // ORDER MATTERS, and this line must stay first.
                        //
                        // Spring Security applies the first matcher that
                        // matches, so a broad public entry placed above this
                        // would make the moderation queue world-readable while
                        // every test that only checks the happy path kept
                        // passing. Declaring the restriction before any
                        // permitAll means a future wildcard cannot silently
                        // open it. AdminFeedbackAuthorisationIT asserts an
                        // anonymous GET here is 401 so the ordering cannot
                        // regress unnoticed.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // Reads the Bearer token and populates the context. Placed
                // before the username/password filter, which is disabled but is
                // still the conventional anchor point in the chain.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Requires X-Requested-With on the two cookie-authenticated
                // POSTs. Anchored to the JWT filter rather than to the
                // username/password filter so the order of the two additions is
                // stated rather than incidental: this runs first, and a request
                // missing the header is refused before any token is parsed.
                //
                // Both sit after Spring Security's CorsFilter, which is what
                // makes the 403 a readable cross-origin response rather than an
                // opaque CORS failure, and what lets the OPTIONS preflight
                // through untouched.
                .addFilterBefore(new CsrfHeaderFilter(apiErrorWriter), JwtAuthenticationFilter.class)
                // Rejections here never reach @RestControllerAdvice, so they are
                // rendered in the section 6 error shape by SecurityErrorHandler.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler));

        // Google sign-in, only when it is actually configured. getIfAvailable()
        // returns null rather than throwing when no GOOGLE_CLIENT_ID is set, so
        // an install without Google credentials starts normally and every other
        // auth path keeps working.
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations != null) {
            AuthenticationSuccessHandler successHandler = oauth2SuccessHandler.getIfAvailable();
            http.oauth2Login(oauth2 -> {
                oauth2.clientRegistrationRepository(registrations);
                if (successHandler != null) {
                    oauth2.successHandler(successHandler);
                }
            });
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        // X-Requested-With is what CsrfHeaderFilter demands on /refresh and
        // /logout, and it is not CORS-safelisted - so without it named here the
        // preflight fails and the browser never sends the real request at all.
        // The endpoint would then be unreachable from the frontend while
        // remaining perfectly reachable from curl, which is the failure mode
        // that looks like the guard working. CsrfHeaderIT pins this.
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", CsrfHeaderFilter.HEADER));
        // A browser will not let script read a response header on a cross-origin
        // response unless the server names it here. Retry-After is on the wire of
        // every 429 already - GlobalExceptionHandler sets it - but without this
        // line fetch() refuses to hand it over, and the frontend's
        // ApiError.retryAfterSeconds is null on exactly the responses it exists
        // for. Shipping a field that is always null is worse than not having one,
        // so CorsExposedHeadersIT fails the build if this is removed.
        config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        // The refresh token is an httpOnly cookie, so credentials must cross.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
