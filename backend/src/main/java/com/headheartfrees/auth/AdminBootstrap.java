package com.headheartfrees.auth;

import com.headheartfrees.config.AuthProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only path to an ADMIN role, and it is not an endpoint.
 *
 * <h2>How the first admin is created</h2>
 *
 * <ol>
 *   <li>Register normally through {@code POST /api/v1/auth/register}.
 *   <li>Set {@code APP_ADMIN_BOOTSTRAP_EMAILS} to that address.
 *   <li>Restart the backend.
 * </ol>
 *
 * <p>This runner then promotes the <em>already existing</em> account. It never
 * creates one, so the environment variable is not a credential and leaking it
 * grants nobody anything - an attacker who sets it still needs the password of
 * an account at that address.
 *
 * <h2>What was rejected</h2>
 *
 * <p>Seeding an admin in a Flyway migration would put a password hash in the
 * repository, and a default admin password in a public repo is the same thing
 * as no admin password.
 *
 * <p>A {@code POST /api/v1/admin/promote} endpoint, however well guarded, is a
 * self-service path to ADMIN, which the phase brief rules out. There is no
 * request body anywhere in this application that carries a role.
 *
 * <p>Idempotent: promoting an account that is already ADMIN does nothing and
 * logs nothing, so a restart loop is not a log flood.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final List<String> bootstrapEmails;

    AdminBootstrap(UserAccountRepository users, AuthProperties properties) {
        this.users = users;
        // Already trimmed and lowercased by AuthProperties' compact constructor.
        this.bootstrapEmails = properties.adminBootstrapEmails();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapEmails.isEmpty()) {
            return;
        }

        for (String email : bootstrapEmails) {
            users.findByEmail(email).ifPresentOrElse(
                    this::promote,
                    // Not a failure. The documented sequence is register, set
                    // the variable, restart - so on a fresh install the address
                    // legitimately does not exist yet, and the operator needs to
                    // be told that rather than left wondering why nothing
                    // happened. Logged without surrounding text that would make
                    // it look like an error.
                    () -> log.info(
                            "APP_ADMIN_BOOTSTRAP_EMAILS lists an address with no account yet; "
                                    + "register it and restart to promote it"));
        }
    }

    private void promote(UserAccount account) {
        if (account.getRole() == UserRole.ADMIN) {
            return;
        }
        account.assignRole(UserRole.ADMIN);
        users.save(account);
        // The id, not the address: this line ends up in aggregated logs.
        log.info("Promoted account {} to ADMIN via APP_ADMIN_BOOTSTRAP_EMAILS", account.getId());
    }
}
