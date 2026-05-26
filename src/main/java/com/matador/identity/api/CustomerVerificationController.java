package com.matador.identity.api;

import com.matador.identity.IdentityService;
import com.matador.identity.api.IdentityDtos.VerificationResponse;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/me/verification")
@Tag(name = "Customer-Profile")
public class CustomerVerificationController {

    private final IdentityService identityService;

    public CustomerVerificationController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Start verification",
        description = "Begin an identity verification session; returns a client_secret.")
    public VerificationResponse start() {
        return identityService.startSession(CurrentUser.requireId());
    }

    @GetMapping
    @Operation(summary = "Verification status", description = "Status of the latest verification session.")
    public VerificationResponse status() {
        return identityService.currentStatus(CurrentUser.requireId());
    }
}
