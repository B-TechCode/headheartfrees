package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** Registration, and the property that it reveals nothing. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistrationIT extends AuthTestSupport {

    @Autowired
    private UserAccountRepository users;

    @Test
    @DisplayName("registration creates exactly one USER account, never an ADMIN")
    void registrationCreatesUser() throws Exception {
        register("new@example.com", PASSWORD, "New Person");

        assertThat(users.count()).isEqualTo(1);
        UserAccount created = users.findByEmail("new@example.com").orElseThrow();
        assertThat(created.getRole())
                .as("Registration must never be able to produce an admin")
                .isEqualTo(UserRole.USER);
        assertThat(created.isEmailVerified())
                .as("Nothing can verify an address; the column starts false")
                .isFalse();
        assertThat(created.getPasswordHash())
                .as("Argon2id, not the password and not BCrypt")
                .startsWith("$argon2id$");
    }

    @Test
    @DisplayName("a duplicate email is indistinguishable from a new one")
    void duplicateDoesNotLeak() throws Exception {
        MvcResult first = performRegister("taken@example.com", PASSWORD);
        MvcResult second = performRegister("taken@example.com", "a different long passphrase");

        assertThat(second.getResponse().getStatus())
                .as("Same status")
                .isEqualTo(first.getResponse().getStatus());
        assertThat(second.getResponse().getContentAsString())
                .as("Same body, byte for byte. A different message here is an "
                        + "account-existence oracle.")
                .isEqualTo(first.getResponse().getContentAsString());

        assertThat(users.count())
                .as("The second attempt must not create or overwrite an account")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("the duplicate attempt does not change the existing password")
    void duplicateDoesNotOverwriteCredentials() throws Exception {
        register("taken@example.com", PASSWORD, "First");
        String originalHash = users.findByEmail("taken@example.com").orElseThrow().getPasswordHash();

        performRegister("taken@example.com", "an attacker chosen passphrase");

        assertThat(users.findByEmail("taken@example.com").orElseThrow().getPasswordHash())
                .as("Registering over an existing address must not be a password reset")
                .isEqualTo(originalHash);
    }

    @Test
    @DisplayName("the shared response says something true in both cases")
    void sharedMessageIsHonest() throws Exception {
        // Phase 6 renders this next to a link to /login. If the wording changes,
        // it has to stay true whether or not an account was created.
        performRegister("someone@example.com", PASSWORD)
                .getResponse();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("other@example.com", PASSWORD, "Other"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(RegistrationResponse.SHARED_MESSAGE));
    }

    @Test
    @DisplayName("email matching is case-insensitive, so CITEXT is doing its job")
    void emailIsCaseInsensitive() throws Exception {
        register("Mixed@Example.com", PASSWORD, "First");
        performRegister("mixed@example.com", "a different long passphrase");

        assertThat(users.count())
                .as("CITEXT must prevent two accounts differing only in case")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a weak password is rejected with a reason, before any account exists")
    void weakPasswordRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("weak@example.com", "short", "Person"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(users.count()).isZero();
    }

    @Test
    @DisplayName("a malformed email is a validation error in the documented shape")
    void malformedEmailRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("not-an-email", PASSWORD, "Person"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    private MvcResult performRegister(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, "Person"))))
                .andExpect(status().isCreated())
                .andReturn();
    }
}
