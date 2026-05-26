package com.matador.payment.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record SetupIntentResponse(String setupIntentId, String clientSecret) {}

    public record AttachPaymentMethodRequest(
        @NotBlank String stripePaymentMethodId, boolean isDefault) {}

    public record PaymentMethodResponse(
        UUID id, String brand, String last4, Integer expMonth, Integer expYear, boolean isDefault) {}
}
