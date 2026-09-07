package com.headheartfrees.feedback;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Rejects markup rather than sanitising it.
 *
 * <p>This content is displayed publicly. Escaping on output is the usual
 * answer and this project does it anyway - React escapes by default and
 * nothing on {@code /voices} uses {@code dangerouslySetInnerHTML} - but a
 * second, simpler rule at the boundary is worth having: a stored value that
 * cannot contain a tag cannot become an injection the day someone adds a
 * plain-text export, an email digest, or an admin view that renders raw.
 *
 * <p><strong>{@code <3} is deliberately allowed.</strong> Rejecting every
 * {@code <} would be simpler still and would fail people writing the most
 * common affectionate thing there is, on a site about feelings. So the rule is
 * tag-shaped {@code <} only: a bracket followed by a letter, a slash, a bang
 * or a question mark. Numeric and named entities are rejected too, so the
 * check cannot be walked around by writing {@code &lt;script&gt;} and waiting
 * for something downstream to decode it.
 */
@Documented
@Constraint(validatedBy = NoHtml.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {

    String message() default
            "Please write this without HTML or angle-bracket tags.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<NoHtml, String> {

        /** `<` followed by what a tag or a comment or a doctype starts with. */
        private static final Pattern TAG_LIKE = Pattern.compile("<[a-zA-Z/!?]");

        /** `&#39;`, `&#x3c;`, `&lt;`, `&gt;`, `&amp;` - anything that decodes. */
        private static final Pattern ENTITY = Pattern.compile("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]{2,10});");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Null and blank are this annotation's business only insofar as
            // they contain no markup. @NotBlank and @Size say whether they are
            // acceptable; one constraint, one question.
            if (value == null) {
                return true;
            }
            return !TAG_LIKE.matcher(value).find() && !ENTITY.matcher(value).find();
        }
    }
}
