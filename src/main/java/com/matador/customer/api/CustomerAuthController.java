package com.matador.customer.api;

import com.matador.customer.CustomerService;
import com.matador.customer.api.CustomerDtos.LoginRequest;
import com.matador.customer.api.CustomerDtos.RefreshRequest;
import com.matador.customer.api.CustomerDtos.RegisterRequest;
import com.matador.customer.api.CustomerDtos.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/auth")
@Tag(name = "Customer-Auth")
public class CustomerAuthController {

    private final CustomerService customerService;

    public CustomerAuthController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Create a customer account and return tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "409", description = "Email or phone already in use"),
        @ApiResponse(responseCode = "422", description = "Validation failed (age, password)")
    })
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return customerService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email and password.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return customerService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Exchange a refresh token for new tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return customerService.refresh(request.refreshToken());
    }
}
