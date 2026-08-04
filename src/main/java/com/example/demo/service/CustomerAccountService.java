package com.example.demo.service;

import com.example.demo.UserRepository;
import com.example.demo.dto.account.CustomerAccountCreateRequest;
import com.example.demo.dto.account.CustomerAccountResponse;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CustomerAccountService {

    private static final Set<String> MANAGED_ROLES = Set.of("CUSTOMER", "ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAccountResponse create(CustomerAccountCreateRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("An account already exists for " + email + ".");
        }

        String role = request.role().trim().toUpperCase(Locale.ROOT);
        if (!MANAGED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be CUSTOMER or ADMIN.");
        }

        User account = new User();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(role);
        return toResponse(userRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> list() {
        return userRepository.findAllByOrderByEmailAsc()
                .stream().map(this::toResponse).toList();
    }

    private CustomerAccountResponse toResponse(User user) {
        return new CustomerAccountResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
