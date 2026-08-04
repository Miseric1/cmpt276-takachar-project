package com.example.demo.controller;

import com.example.demo.dto.account.CustomerAccountCreateRequest;
import com.example.demo.dto.account.CustomerAccountResponse;
import com.example.demo.service.CustomerAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Customer credential management performed by the administrator acting as SPOC. */
@RestController
@RequestMapping("/api/admin/customers")
public class CustomerAccountController {

    private final CustomerAccountService accountService;

    public CustomerAccountController(CustomerAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<CustomerAccountResponse> create(
            @Valid @RequestBody CustomerAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @GetMapping
    public List<CustomerAccountResponse> list() {
        return accountService.list();
    }
}
