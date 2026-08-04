package com.example.demo;

import com.example.demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerAccountApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void adminActingAsSpocCanOpenCustomerAccessPage() throws Exception {
        mockMvc.perform(get("/admin/customers")
                        .with(user("spoc@takachar.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customers"));
    }

    @Test
    void adminActingAsSpocCreatesAndListsCustomerWithoutExposingPassword() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .with(user("spoc@takachar.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Customer@Example.com\",\"password\":\"temporary123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("temporary123");
        assertThat(passwordEncoder.matches("temporary123", stored.getPassword())).isTrue();

        mockMvc.perform(get("/api/admin/customers")
                        .with(user("spoc@takachar.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'customer@example.com')]").exists());
    }

    @Test
    void duplicateCustomerEmailIsRejectedCaseInsensitively() throws Exception {
        String body = "{\"email\":\"duplicate@example.com\",\"password\":\"temporary123\"}";
        mockMvc.perform(post("/api/admin/customers")
                        .with(user("spoc@takachar.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/customers")
                        .with(user("spoc@takachar.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"DUPLICATE@example.com\",\"password\":\"temporary456\"}"))
                .andExpect(status().isConflict());
    }
}
