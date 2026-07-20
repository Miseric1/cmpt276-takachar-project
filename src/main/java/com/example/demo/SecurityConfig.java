package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

   @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(isAdmin ? "/admin/home" : "/customer/home");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // --- Existing page + asset rules (unchanged) ---------------
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**").permitAll()

                // --- New REST API rules ------------------------------------
                // Admin-only reads must come before the public GET rules so a
                // more specific "/admin" or "/versions" path is not swallowed
                // by the broader public matcher.
                .requestMatchers(HttpMethod.GET,
                        "/api/knowledge/admin/**", "/api/knowledge/*/versions/**").hasRole("ADMIN")

                // Public engagement (helpful / not-helpful votes).
                .requestMatchers(HttpMethod.POST,
                        "/api/knowledge/*/helpful", "/api/knowledge/*/not-helpful").permitAll()

                // Public reads: published Knowledge Base content and the shared vocab.
                .requestMatchers(HttpMethod.GET,
                        "/api/knowledge", "/api/knowledge/*",
                        "/api/categories", "/api/categories/*",
                        "/api/tags").permitAll()

                // Content and category mutations are staff-only.
                .requestMatchers(HttpMethod.POST, "/api/knowledge", "/api/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/knowledge/**", "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/knowledge/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/knowledge/**", "/api/categories/**").hasRole("ADMIN")

                // The staff dashboard analytics are entirely admin-only.
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                // --- Existing catch-all rules (unchanged) ------------------
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            // REST clients authenticate with the shared session but cannot send
            // Spring's CSRF token, so CSRF protection is scoped to the stateful
            // form-based pages and lifted for the stateless JSON API.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            // Unauthenticated API calls get 401 JSON semantics instead of the
            // browser login redirect used for the Thymeleaf pages.
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")))
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(successHandler())
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }
}
