package com.headheartfrees.vent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The automated guard for PROJECT_BRIEF.md rule 2.1.
 *
 * <p>For four phases that rule was enforced by comments and Javadoc, and the
 * phase log flagged twice that a rule with no test behind it is one refactor
 * away from being broken silently. This is that test.
 *
 * <p>What it defends: <em>vent text is never transmitted.</em> The attack it
 * anticipates is not malice but drift — somebody adds a {@code String note} to
 * a request DTO for a good-sounding reason, nobody reads the Javadoc, and the
 * product quietly starts receiving what it promised never to receive. Each rule
 * below fails the build instead.
 *
 * <p>If one of these fails, the fix is almost never to relax the rule.
 */
class VentRuleArchitectureTest {

    private static final String VENT_PACKAGE = "com.headheartfrees.vent";

    private static final JavaClasses VENT_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(VENT_PACKAGE);

    @Test
    @DisplayName("no type in the vent package declares a String field")
    void noStringFieldsInVentPackage() {
        // A String field anywhere in this package is the shape a free-text
        // field takes. `mood` is an enum precisely so this rule can be absolute
        // rather than needing an exception list that grows over time.
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAPackage(VENT_PACKAGE)
                .and().areNotStatic()
                .should().notHaveRawType(String.class)
                .because("PROJECT_BRIEF.md 2.1: the vent domain must never carry free text. "
                        + "A String field here is how that rule gets broken by accident.");

        rule.check(VENT_CLASSES);
    }

    @Test
    @DisplayName("the release request carries exactly one component, and it is the mood enum")
    void releaseRequestShapeIsLocked() {
        RecordComponent[] components = ReleaseRequest.class.getRecordComponents();

        assertThat(components)
                .as("ReleaseRequest must stay a single-field record. Adding a second "
                        + "field is the change rule 2.1 exists to prevent.")
                .hasSize(1);

        assertThat(components[0].getType())
                .as("The only component must be the closed Mood enum, never a String")
                .isEqualTo(Mood.class);
        assertThat(components[0].getName()).isEqualTo("mood");
    }

    @Test
    @DisplayName("the persisted entity holds no free text either")
    void ventEventHasNoStringColumns() {
        // Covered by the package-wide rule above, asserted separately because
        // this is the class where a stored String would do lasting damage: a
        // request field leaks one message, a column accumulates them.
        assertThat(VentEvent.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == String.class);
    }

    @Test
    @DisplayName("nothing in the vent package can reach a servlet request or a logger argument that is a body")
    void ventPackageDoesNotTouchRawRequestBodies() {
        // Reading the raw request stream would sidestep the DTO entirely, which
        // is the one route by which text could arrive despite every rule above.
        ArchRule rule = noClasses()
                .that().resideInAPackage(VENT_PACKAGE)
                .should().callMethod(jakarta.servlet.ServletRequest.class, "getReader")
                .orShould().callMethod(jakarta.servlet.ServletRequest.class, "getInputStream")
                .because("Reading the raw body bypasses ReleaseRequest and would let vent "
                        + "text reach the server despite the DTO being closed.");

        rule.check(VENT_CLASSES);
    }
}
