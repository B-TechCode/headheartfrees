package com.headheartfrees;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The dependency direction between {@code auth} and {@code vent}, enforced.
 *
 * <p>{@code VentRuleArchitectureTest} guards what a vent type may <em>contain</em>
 * - no String fields, no raw body access. This guards what the two modules may
 * <em>see</em> of each other, which is a different failure and needs its own
 * rule.
 *
 * <h2>What this defends</h2>
 *
 * PROJECT_BRIEF.md rule 2.2: venting requires no account. The way that rule
 * dies is not by someone deciding to require login. It dies by degrees - an
 * import, then an {@code Optional<UserId>} parameter "just for analytics", then
 * a nullable {@code user_id} column, and at no point does anyone believe they
 * have changed the product. The first of those steps is an import, so that is
 * what fails here.
 *
 * <p>The rule runs in <strong>both</strong> directions as two separate tests,
 * so a failure names which way the boundary was crossed rather than just
 * reporting that it was.
 *
 * <p>If one of these fails, the fix is not to relax the rule or add an
 * exception. If vent genuinely needs something from auth, the answer is that it
 * does not: the vent endpoints are public, and a vent row records a mood and a
 * timestamp.
 */
class ModuleBoundaryArchitectureTest {

    private static final String AUTH = "com.headheartfrees.auth..";
    private static final String VENT = "com.headheartfrees.vent..";

    /**
     * Both packages, imported together. Importing only one would make every
     * cross-package dependency unresolvable and quietly pass.
     */
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.headheartfrees");

    @Test
    @DisplayName("nothing in vent may depend on anything in auth")
    void ventDoesNotDependOnAuth() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(VENT)
                .should().dependOnClassesThat().resideInAPackage(AUTH)
                .because("PROJECT_BRIEF.md rule 2.2: venting requires no account. A vent type "
                        + "that can see an auth type is one refactor away from recording who "
                        + "vented, and vent_events must never gain a user_id.");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("nothing in auth may depend on anything in vent")
    void authDoesNotDependOnVent() {
        // The reverse direction is forbidden for a different reason: it is the
        // module boundary in PROJECT_BRIEF.md section 4. Auth reaching into
        // vent - to count someone's releases, say - would create exactly the
        // link between an identity and a release that the vent schema is shaped
        // to prevent, and would stop either module being liftable on its own.
        ArchRule rule = noClasses()
                .that().resideInAPackage(AUTH)
                .should().dependOnClassesThat().resideInAPackage(VENT)
                .because("PROJECT_BRIEF.md section 4: modules talk through service interfaces "
                        + "and IDs. Auth has no business reading vent data, and joining an "
                        + "identity to a release is the specific thing rule 2.1 forbids.");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("no JPA entity in auth references a vent type, in either direction")
    void noCrossModuleJpaRelationships() {
        // Narrower than the package rules above and kept separate because it is
        // the concrete mistake section 4 names: a @ManyToOne across domains.
        // The package rules already catch it; this one fails with a message
        // that says what the reader actually did.
        ArchRule rule = noClasses()
                .that().resideInAPackage(AUTH)
                .should().dependOnClassesThat().haveSimpleName("VentEvent")
                .because("No cross-module JPA relationships (PROJECT_BRIEF.md section 4). "
                        + "Reference other domains by ID, never by association.");

        rule.check(CLASSES);
    }
}
