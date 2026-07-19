package com.example.demo;
import com.example.demo.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // Dashboard analytics: how many customers vs staff exist.
    long countByRoleIgnoreCase(String role);
}