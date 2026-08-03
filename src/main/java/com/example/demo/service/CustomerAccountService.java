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

@Service
public class CustomerAccountService {

    private static final String CUSTOMER_ROLE = "CUSTOMER";

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

        User customer = new User();
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(request.password()));
        customer.setRole(CUSTOMER_ROLE);
        return toResponse(userRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> list() {
        return userRepository.findAllByRoleIgnoreCaseOrderByEmailAsc(CUSTOMER_ROLE)
                .stream().map(this::toResponse).toList();
    }

    private CustomerAccountResponse toResponse(User user) {
        return new CustomerAccountResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
