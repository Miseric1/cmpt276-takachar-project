package com.example.demo;

import com.example.demo.model.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the account-creation test cases documented under Story 1 (User
 * Registration), as that story exists after Story 13 removed public
 * self-registration.
 *
 * <p>Accounts are no longer created by customers through a registration form;
 * the application has no /register endpoint at all. Instead an admin acting as
 * SPOC provisions accounts through POST /api/admin/customers. Each documented
 * case is therefore exercised against that flow:
 *
 * <ul>
 *   <li>"Register with valid information" — {@link ValidProvisioning}, which
 *       also verifies the second half of the expected result, that the new
 *       user can actually log in.</li>
 *   <li>"Register with an existing email" — {@link DuplicateEmail}.</li>
 *   <li>"Submit empty registration fields" — {@link InvalidFields}.</li>
 *   <li>"Submit mismatching passwords" — not applicable. The admin
 *       provisioning request carries a single password field with no
 *       confirmation, so there is no mismatch condition to reject. The case
 *       only existed for the removed self-registration form.</li>
 * </ul>
 *
 * <p>{@link RegistrationRemoved} covers the Story 13 acceptance criteria that
 * make the above true.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountProvisioningTest {

    private static final String ADMIN = "spoc@takachar.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    /** Provisions an account as the admin and asserts it was created. */
    private void provision(String email, String password) throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .with(user(ADMIN).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, password)))
                .andExpect(status().isCreated());
    }

    /** Attempts to provision an account and asserts the request was rejected. */
    private void expectRejected(String body) throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .with(user(ADMIN).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Nested
    class ValidProvisioning {

        @Test
        void accountIsCreatedWithCustomerRoleAndAHashedPassword() throws Exception {
            provision("newcustomer@example.com", "temporary123");

            User stored = userRepository.findByEmail("newcustomer@example.com").orElseThrow();
            assertThat(stored.getRole()).isEqualTo("CUSTOMER");
            assertThat(stored.getPassword()).isNotEqualTo("temporary123");
            assertThat(passwordEncoder.matches("temporary123", stored.getPassword())).isTrue();
        }

        @Test
        void theNewUserCanLogInWithTheProvidedCredentials() throws Exception {
            provision("loginable@example.com", "temporary123");

            mockMvc.perform(formLogin("/login")
                            .userParameter("email")
                            .user("loginable@example.com")
                            .password("temporary123"))
                    .andExpect(authenticated().withRoles("CUSTOMER"));
        }

        @Test
        void loggingInSendsTheNewUserToTheCustomerDashboard() throws Exception {
            provision("routed@example.com", "temporary123");

            mockMvc.perform(formLogin("/login")
                            .userParameter("email")
                            .user("routed@example.com")
                            .password("temporary123"))
                    .andExpect(redirectedUrl("/customer/home"));
        }

        @Test
        void theWrongPasswordDoesNotAuthenticateTheNewUser() throws Exception {
            provision("wrongpass@example.com", "temporary123");

            mockMvc.perform(formLogin("/login")
                            .userParameter("email")
                            .user("wrongpass@example.com")
                            .password("not-the-password"))
                    .andExpect(unauthenticated());
        }

        @Test
        void theEmailIsStoredInLowercaseRegardlessOfHowItWasTyped() throws Exception {
            provision("MixedCase@Example.COM", "temporary123");

            assertThat(userRepository.findByEmail("mixedcase@example.com")).isPresent();
        }

        /**
         * Known defect. CustomerAccountService.create lowercases the address
         * before storing it, but CustomUserDetailsService.loadUserByUsername
         * looks the user up with the case-sensitive findByEmail. A customer
         * given credentials containing any uppercase letter therefore cannot
         * log in with the address their SPOC typed, and the login page reports
         * only a generic failure.
         *
         * <p>Fix: have loadUserByUsername use findByEmailIgnoreCase, which
         * already exists on UserRepository and is what the duplicate check
         * uses. Re-enable this test once that lands.
         */
        @Test
        @Disabled("Defect: login is case-sensitive but stored emails are lowercased")
        void theNewUserCanLogInWithTheEmailExactlyAsTheAdminTypedIt() throws Exception {
            provision("MixedCase@Example.COM", "temporary123");

            mockMvc.perform(formLogin("/login")
                            .userParameter("email")
                            .user("MixedCase@Example.COM")
                            .password("temporary123"))
                    .andExpect(authenticated().withRoles("CUSTOMER"));
        }
    }

    @Nested
    class DuplicateEmail {

        @Test
        void aSecondAccountForTheSameEmailIsRejected() throws Exception {
            provision("taken@example.com", "temporary123");

            mockMvc.perform(post("/api/admin/customers")
                            .with(user(ADMIN).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("taken@example.com", "different456")))
                    .andExpect(status().isConflict());
        }

        @Test
        void theRejectionExplainsThatTheEmailIsAlreadyRegistered() throws Exception {
            provision("explain@example.com", "temporary123");

            String response = mockMvc.perform(post("/api/admin/customers")
                            .with(user(ADMIN).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("explain@example.com", "different456")))
                    .andExpect(status().isConflict())
                    .andReturn().getResponse().getContentAsString();

            assertThat(response).contains("explain@example.com");
            assertThat(response.toLowerCase()).contains("already exists");
        }

        @Test
        void theExistingAccountKeepsItsOriginalPassword() throws Exception {
            provision("unchanged@example.com", "original123");

            mockMvc.perform(post("/api/admin/customers")
                            .with(user(ADMIN).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("unchanged@example.com", "hijack456")))
                    .andExpect(status().isConflict());

            User stored = userRepository.findByEmail("unchanged@example.com").orElseThrow();
            assertThat(passwordEncoder.matches("original123", stored.getPassword())).isTrue();
            assertThat(passwordEncoder.matches("hijack456", stored.getPassword())).isFalse();
        }

        @Test
        void duplicatesAreDetectedRegardlessOfEmailCasing() throws Exception {
            provision("casing@example.com", "temporary123");

            mockMvc.perform(post("/api/admin/customers")
                            .with(user(ADMIN).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("CASING@Example.com", "different456")))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class InvalidFields {

        @Test
        void anEmptyRequestBodyIsRejected() throws Exception {
            expectRejected("{}");
        }

        @Test
        void aBlankEmailIsRejected() throws Exception {
            expectRejected(json("", "temporary123"));
        }

        @Test
        void aWhitespaceOnlyEmailIsRejected() throws Exception {
            expectRejected(json("   ", "temporary123"));
        }

        @Test
        void aBlankPasswordIsRejected() throws Exception {
            expectRejected(json("blankpass@example.com", ""));
        }

        @Test
        void anEmailWithoutAnAtSignIsRejected() throws Exception {
            expectRejected(json("not-an-email", "temporary123"));
        }

        @Test
        void aPasswordShorterThanTheMinimumIsRejected() throws Exception {
            expectRejected(json("shortpass@example.com", "short"));
        }

        @Test
        void aRejectedRequestCreatesNoAccount() throws Exception {
            long before = userRepository.count();

            expectRejected(json("", ""));
            expectRejected(json("also-not-an-email", "temporary123"));
            expectRejected(json("nostored@example.com", "tiny"));

            assertThat(userRepository.count()).isEqualTo(before);
            assertThat(userRepository.findByEmail("nostored@example.com")).isEmpty();
        }
    }

    @Nested
    class RegistrationRemoved {

        @Test
        void thereIsNoPublicSelfServiceRegistrationPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus())
                                    .as("/register must not serve a registration form")
                                    .isNotEqualTo(200));
        }

        @Test
        void anAnonymousVisitorCannotProvisionAnAccount() throws Exception {
            mockMvc.perform(post("/api/admin/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("anon@example.com", "temporary123")))
                    .andExpect(status().isUnauthorized());

            assertThat(userRepository.findByEmail("anon@example.com")).isEmpty();
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void aCustomerCannotProvisionAnAccountForSomeoneElse() throws Exception {
            mockMvc.perform(post("/api/admin/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("byacustomer@example.com", "temporary123")))
                    .andExpect(status().isForbidden());

            assertThat(userRepository.findByEmail("byacustomer@example.com")).isEmpty();
        }

        @Test
        void anExistingCustomerCanStillLogIn() throws Exception {
            provision("existing@example.com", "temporary123");

            mockMvc.perform(formLogin("/login")
                            .userParameter("email")
                            .user("existing@example.com")
                            .password("temporary123"))
                    .andExpect(authenticated());
        }
    }
}
