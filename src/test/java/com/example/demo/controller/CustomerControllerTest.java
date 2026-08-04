package com.example.demo.controller;

import com.example.demo.model.Feedback;
import com.example.demo.model.SubmissionType;
import com.example.demo.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc
@WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @Test
    void customerCanOpenComplaintForm() throws Exception {
        mockMvc.perform(get("/customer/complaint"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-complaint"))
                .andExpect(model().attribute("email", "customer@example.com"));
    }

    @Test
    void complaintFormCreatesComplaintOwnedBySignedInCustomer() throws Exception {
        mockMvc.perform(post("/customer/complaint")
                        .with(csrf())
                        .param("category", "LOGISTICS")
                        .param("project", "Biomass pickup")
                        .param("account", "Example Farm")
                        .param("description", "The scheduled pickup did not arrive."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/complaint?submitted"));

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackService).createFeedback(captor.capture());

        Feedback complaint = captor.getValue();
        assertThat(complaint.getType()).isEqualTo(SubmissionType.COMPLAINT);
        assertThat(complaint.getCreatedBy()).isEqualTo("customer@example.com");
        assertThat(complaint.getCategory()).isEqualTo("LOGISTICS");
        assertThat(complaint.getProject()).isEqualTo("Biomass pickup");
        assertThat(complaint.getAccount()).isEqualTo("Example Farm");
        assertThat(complaint.getDescription()).isEqualTo("The scheduled pickup did not arrive.");
    }
}
