package com.matador.payment.api;

import com.matador.payment.PaymentService;
import com.matador.payment.api.PaymentDtos.AttachPaymentMethodRequest;
import com.matador.payment.api.PaymentDtos.PaymentMethodResponse;
import com.matador.payment.api.PaymentDtos.SetupIntentResponse;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/me/payment-methods")
@Tag(name = "Customer-Profile")
public class PaymentMethodController {

    private final PaymentService paymentService;

    public PaymentMethodController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "List payment methods", description = "Saved cards for the customer.")
    public List<PaymentMethodResponse> list() {
        return paymentService.listPaymentMethods(CurrentUser.requireId());
    }

    @PostMapping("/setup")
    @Operation(summary = "Create setup intent", description = "Begin saving a card; returns client_secret.")
    public SetupIntentResponse setup() {
        return paymentService.createSetupIntent(CurrentUser.requireId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach card", description = "Attach a confirmed payment method to the customer.")
    public PaymentMethodResponse attach(@Valid @RequestBody AttachPaymentMethodRequest request) {
        return paymentService.attachPaymentMethod(
            CurrentUser.requireId(), request.stripePaymentMethodId(), request.isDefault());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Detach card", description = "Remove a saved payment method.")
    public void detach(@PathVariable UUID id) {
        paymentService.detachPaymentMethod(CurrentUser.requireId(), id);
    }

    @PostMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set default card", description = "Mark a payment method as default.")
    public void setDefault(@PathVariable UUID id) {
        paymentService.setDefaultPaymentMethod(CurrentUser.requireId(), id);
    }
}
