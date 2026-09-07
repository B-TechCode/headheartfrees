package com.headheartfrees.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A submitted note.
 *
 * <p>There is no {@code status} field and no {@code userId} field. Neither is
 * an oversight: the status is always PENDING on creation, and the user id comes
 * from the access token if there is one, never from the body. A request cannot
 * ask to be published and cannot claim to be somebody.
 *
 * @param rating      1-5, required. Also CHECK-constrained in the database.
 * @param message     10-2000 characters. The floor is 10 rather than 20 because
 *                    "Thank you." is ten and "This helped a lot." is nineteen,
 *                    and on this page those are the likeliest honest answers.
 * @param displayName optional. Whatever the person wants to be called here,
 *                    which need not be their account name and need not exist.
 * @param location    optional, one short line. See the field constraints for
 *                    why commas are refused.
 */
public record SubmitFeedbackRequest(

        @NotNull(message = "Please choose a rating from 1 to 5.")
        @Min(value = 1, message = "Please choose a rating from 1 to 5.")
        @Max(value = 5, message = "Please choose a rating from 1 to 5.")
        Short rating,

        @NotBlank(message = "Please write a message.")
        @Size(min = 10, max = 2000,
                message = "Please write between 10 and 2000 characters.")
        @NoHtml
        String message,

        @Size(max = 60, message = "Please keep this to 60 characters or fewer.")
        @Pattern(regexp = "[^\r\n]*", message = "Please keep this on one line.")
        @NoHtml
        String displayName,

        /*
         * One line, 60 characters, no commas.
         *
         * The comma rule is what stops this becoming a list. "Mumbai" is a
         * place; "Mumbai, Maharashtra, India, Earth" is an address being built
         * in a field that is displayed next to somebody's name on a public
         * page. The placeholder in the form teaches the format rather than
         * explaining the rule, and the form says so before it is submitted.
         */
        @Size(max = 60, message = "Please keep this to 60 characters or fewer.")
        @Pattern(regexp = "[^,\r\n]*",
                message = "Please give a single place, without commas.")
        @NoHtml
        String location) {
}
