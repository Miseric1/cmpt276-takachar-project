package com.example.demo;

import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.User;
import com.example.demo.service.KnowledgeService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code seedFaqArticles} below is gated by the {@code app.seed-demo-data}
 * property (see {@code @ConditionalOnProperty}) rather than a Spring profile,
 * because no test in this project activates a "test" profile — a
 * {@code @Profile("!test")} guard would silently never trigger. Setting
 * {@code app.seed-demo-data=false} in src/test/resources/application.properties
 * disables it for every test in the suite, with no changes needed to
 * individual test classes. Without this, the 18 seeded articles leak into
 * whatever database a test's Spring context uses, making keyword/category
 * search assertions (e.g. "first result" or "duplicate title") match
 * unexpected extra articles.
 *
 * {@code createAdmin} is intentionally left unguarded — there's no evidence
 * it causes test failures, and disabling it risks silently breaking some
 * other test that depends on the seeded admin user existing.
 */

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@test.com");
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("Admin user created: admin@test.com / password123");
            }
        };
    }

    /**
     * Seeds placeholder FAQ / Knowledge Base articles that don't already exist,
     * so the FAQ page has display-ready content for this iteration. Checked by
     * title rather than "is the table empty," so a manually-created test
     * article won't cause the rest of the placeholders to be skipped. These are
     * created through the same KnowledgeService the REST API uses, so they
     * behave as ordinary articles afterward — an admin can edit or delete any
     * of them from the FAQ page exactly like one they created themselves.
     */
    @Bean
    @ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner seedFaqArticles(KnowledgeService knowledgeService) {
        return args -> {
            // Check per-article rather than "does anything exist" — otherwise a
            // single manually-created test article (from trying out Add Article)
            // would cause every placeholder below to be skipped, not just that one.
            Set<String> existingTitles = knowledgeService
                    .search(null, null, null, null, null, PageRequest.of(0, 500))
                    .getContent()
                    .stream()
                    .map(KnowledgeSummary::title)
                    .map(title -> title.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            int created = 0;

            for (String[] entry : PLACEHOLDER_ARTICLES) {
                String title = entry[0];
                String category = entry[1];
                String body = entry[2];

                if (existingTitles.contains(title.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                KnowledgeRequest request = new KnowledgeRequest(
                        title,
                        null,
                        body,
                        category,
                        Set.of(),
                        Set.of(),
                        "admin@test.com",
                        PublicationStatus.PUBLISHED);

                knowledgeService.create(request, "system");
                created++;
            }

            if (created > 0) {
                System.out.println("Seeded " + created + " placeholder FAQ articles.");
            }
        };
    }

    private static final String[][] PLACEHOLDER_ARTICLES = {

            // Getting Started
            {
                "How do I set up my Takachar customer account?",
                "Getting Started",
                "After your onboarding call, you'll receive an email invite to create your account. Follow the link, set a password, and confirm the farm or facility location associated with your account. If you don't see the invite within one business day, check your spam folder or contact your account manager."
            },
            {
                "What information do I need before requesting my first biomass pickup?",
                "Getting Started",
                "Have your estimated residue volume (in kg or tonnes), the type of crop or forest residue, and your pickup location on hand. If this is your first pickup, our logistics team will also confirm accessibility for the collection vehicle before scheduling."
            },
            {
                "Where can I find training materials for my processing unit?",
                "Getting Started",
                "Every unit ships with a printed quick-start guide, and full training videos are available under your account's Resources tab once your account is active. If you'd prefer in-person training, ask your account manager about scheduling a site visit."
            },

            // Account & Access
            {
                "How do I reset my password?",
                "Account & Access",
                "From the login page, select the password reset option below the login form. You'll receive a reset link at your registered email address, valid for 30 minutes."
            },
            {
                "Can multiple team members share one company account?",
                "Account & Access",
                "Each individual needs their own login for audit and accountability purposes, but multiple users can be linked to the same company or farm profile. Contact support to add a teammate to your organization."
            },
            {
                "How do I update my contact or farm location details?",
                "Account & Access",
                "Go to Customer Account from the sidebar and select \"Edit details.\" Location changes for active pickup schedules may take up to 24 hours to sync with our logistics team."
            },

            // Equipment & Hardware
            {
                "What do the indicator lights on my Takachar unit mean?",
                "Equipment & Hardware",
                "Solid green means normal operation, blinking amber indicates the feed hopper needs attention, and solid red means the unit has stopped and requires a manual reset. A full light reference chart is included in your quick-start guide."
            },
            {
                "How often should I clean or service my processing unit?",
                "Equipment & Hardware",
                "We recommend clearing the feed hopper and ash tray daily, and a full internal cleaning every 200 operating hours. Units under warranty are also eligible for a free annual service visit — ask your account manager to schedule one."
            },
            {
                "What should I do if my unit won't power on?",
                "Equipment & Hardware",
                "First check that the power cable and battery (if applicable) are securely connected. If the unit still won't start, do not attempt to open the housing yourself — submit a support ticket with your unit's serial number so our technical team can diagnose it remotely."
            },

            // Biomass Pickup & Logistics
            {
                "How do I schedule a biomass pickup?",
                "Biomass Pickup & Logistics",
                "Pickups are scheduled through your account manager or the logistics request form linked in your welcome email. Typical lead time is 3-5 business days, depending on your region."
            },
            {
                "What types of crop or forest residue are accepted?",
                "Biomass Pickup & Logistics",
                "We accept most dry agricultural residues (rice husk, groundnut shell, corn stover) and forest residues (branches, sawdust). Wet or contaminated material may be declined at pickup — see the Acceptable Residue Guide for a full list."
            },
            {
                "Can I reschedule or cancel a scheduled pickup?",
                "Biomass Pickup & Logistics",
                "Yes, pickups can be rescheduled free of charge up to 24 hours in advance. Cancellations made less than 24 hours before the scheduled time may be subject to a small logistics fee."
            },

            // Troubleshooting
            {
                "My unit is producing less biochar than expected. What could be wrong?",
                "Troubleshooting",
                "Reduced output is most often caused by high moisture content in the feedstock or an underfilled hopper. Try pre-drying residue for 24-48 hours before processing, and confirm the hopper is loaded to the recommended fill line."
            },
            {
                "I'm seeing an error code on the display. What does it mean?",
                "Troubleshooting",
                "Error codes starting with \"E1\" relate to temperature sensors, \"E2\" to the feed motor, and \"E3\" to airflow. Note the exact code and submit a support ticket — our team can often resolve E1 and E3 codes with a remote reset."
            },
            {
                "The unit is overheating during operation. What should I check?",
                "Troubleshooting",
                "Overheating is usually due to blocked air vents or extended continuous operation beyond the rated duty cycle. Power down the unit, clear any debris from the vents, and allow it to cool for at least 30 minutes before restarting."
            },

            // Billing & Payments
            {
                "How is payment calculated for delivered biomass?",
                "Billing & Payments",
                "Payment is based on the verified dry weight of accepted material at the time of pickup, multiplied by the current rate for that residue type. Rates are reviewed quarterly and posted in your account's Billing tab."
            },
            {
                "When will I receive payment after a pickup?",
                "Billing & Payments",
                "Payments are processed within 7-10 business days of a completed and verified pickup. You'll receive an email confirmation once the payment has been issued."
            },
            {
                "How do I update my bank or payment details?",
                "Billing & Payments",
                "Go to Customer Account > Billing > Payment Details. Changes are verified manually for security and may take up to two business days to take effect."
            }
    };
}