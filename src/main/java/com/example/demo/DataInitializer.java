package com.example.demo;

import com.example.demo.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
