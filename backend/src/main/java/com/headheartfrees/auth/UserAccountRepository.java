package com.headheartfrees.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Package-private, per PROJECT_BRIEF.md section 4. Spring Data proxies
 * package-private interfaces without complaint, so the module boundary costs
 * nothing here.
 */
interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /**
     * Case-insensitive lookup, and the explicit cast is why.
     *
     * <h2>Why a derived query does not work here</h2>
     *
     * A plain {@code findByEmail(String)} compiles, runs, and is
     * <strong>case-sensitive</strong> against a CITEXT column. The JDBC driver
     * sends the parameter with an explicit VARCHAR type, so Postgres resolves
     * {@code citext = varchar} by coercing the citext side down to {@code text}
     * and comparing as text. The column type is doing nothing at that point.
     *
     * <p>The failure is quiet in the worst way: the unique index is still
     * genuinely case-insensitive, so an application that looks up with the
     * wrong case decides the address is free, inserts, and gets a constraint
     * violation as a 500. That is what {@code emailIsCaseInsensitive} caught.
     *
     * <p>Casting the parameter to citext restores citext's own equality
     * operator and lets the query use the unique index. Do not replace this
     * with {@code findByEmailIgnoreCase} either - that generates
     * {@code upper(email)}, which cannot use the index at all.
     *
     * <p>Native, and therefore Postgres-specific. So is the migration that
     * creates the extension.
     */
    @Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
    Optional<UserAccount> findByEmail(@Param("email") String email);

    /** Case-insensitive for the same reason as {@link #findByEmail}. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = CAST(:email AS citext))",
            nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    /** {@code google_id} is plain TEXT, so a derived query is correct here. */
    Optional<UserAccount> findByGoogleId(String googleId);
}
