package com.matador.customer.api;

import com.matador.customer.CustomerService;
import com.matador.customer.CustomerStatus;
import com.matador.customer.VerificationStatus;
import com.matador.customer.api.CustomerDtos.AdminCustomerDetail;
import com.matador.customer.api.CustomerDtos.AdminCustomerSummary;
import com.matador.customer.api.CustomerDtos.UpdateCustomerStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customers")
@Tag(name = "Admin-Customers")
public class AdminCustomerController {

    private final CustomerService customerService;

    public AdminCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
    @Operation(summary = "List customers", description = "Paginated, filterable customer list.")
    public Page<AdminCustomerSummary> list(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) VerificationStatus verificationStatus,
        @RequestParam(required = false) CustomerStatus status,
        @PageableDefault(size = 20) Pageable pageable) {
        return customerService.search(email, phone, verificationStatus, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
    @Operation(summary = "Customer detail", description = "Full customer profile.")
    public AdminCustomerDetail detail(@PathVariable UUID id) {
        return customerService.getDetail(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    @Operation(summary = "Update status", description = "Suspend, deactivate, or reactivate a customer.")
    public AdminCustomerDetail updateStatus(
        @PathVariable UUID id, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return customerService.updateStatus(id, request.status(), request.reason());
    }
}
