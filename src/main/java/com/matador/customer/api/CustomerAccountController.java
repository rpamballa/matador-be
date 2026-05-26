package com.matador.customer.api;

import com.matador.customer.CustomerService;
import com.matador.customer.api.CustomerDtos.AddressRequest;
import com.matador.customer.api.CustomerDtos.AddressResponse;
import com.matador.customer.api.CustomerDtos.CustomerProfileResponse;
import com.matador.customer.api.CustomerDtos.UpdateProfileRequest;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/me")
@Tag(name = "Customer-Profile")
public class CustomerAccountController {

    private final CustomerService customerService;

    public CustomerAccountController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Current profile", description = "Profile of the authenticated customer.")
    public CustomerProfileResponse me() {
        return customerService.getProfile(CurrentUser.requireId());
    }

    @PatchMapping
    @Operation(
        summary = "Update profile",
        description = "Update name and phone. Email and date of birth are immutable.")
    public CustomerProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return customerService.updateProfile(CurrentUser.requireId(), request);
    }

    @GetMapping("/addresses")
    @Operation(summary = "List addresses", description = "Saved addresses for the customer.")
    public List<AddressResponse> addresses() {
        return customerService.listAddresses(CurrentUser.requireId());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add address", description = "Save a new address.")
    public AddressResponse addAddress(@Valid @RequestBody AddressRequest request) {
        return customerService.addAddress(CurrentUser.requireId(), request);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove address", description = "Delete a saved address.")
    public void removeAddress(@PathVariable UUID id) {
        customerService.removeAddress(CurrentUser.requireId(), id);
    }
}
