package com.headheartfrees.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Argon2id, at the parameters OWASP currently recommends.
 *
 * <h2>Why not BCrypt</h2>
 *
 * BCrypt silently truncates its input at 72 bytes. The password policy allows
 * well over 64 characters, and 64 characters of any multi-byte script exceeds
 * 72 bytes comfortably - at which point two different passphrases hash
 * identically and nothing reports a problem. Argon2id has no such limit.
 *
 * <h2>The parameters</h2>
 *
 * m=19456 KiB (19 MiB), t=2, p=1, with a 16-byte salt and a 32-byte hash. This
 * is the OWASP Password Storage Cheat Sheet's first recommended Argon2id
 * configuration. Spring's {@code defaultsForSpringSecurity_v5_8()} uses 16 MiB
 * and is close, but is spelled out here rather than inherited, because a
 * library default changing underneath a password hash is the kind of upgrade
 * that is discovered late.
 *
 * <p>Requires BouncyCastle on the classpath; {@link Argon2PasswordEncoder}
 * delegates to it and fails at construction without it.
 *
 * <p>Cost is roughly 40-60ms per hash on ordinary hardware. That is the point
 * on the login path, and it is also why the test suite is slower from phase 5
 * onward.
 */
@Configuration
public class PasswordEncoderConfig {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BYTES = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY_KIB = 19456;
    private static final int ITERATIONS = 2;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                SALT_LENGTH_BYTES, HASH_LENGTH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS);
    }
}
