package com.matador.customer.api;

import com.matador.customer.CustomerStatus;
import com.matador.customer.VerificationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Request/response DTOs for the customer-facing and admin customer APIs. */
public final class CustomerDtos {

    private CustomerDtos() {}

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String phone,
        @NotBlank @Size(min = 12, max = 100) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @Past LocalDate dateOfBirth) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(
        String accessToken, String refreshToken, long expiresIn, String tokenType) {

        public static TokenResponse bearer(String access, String refresh, long expiresIn) {
            return new TokenResponse(access, refresh, expiresIn, "Bearer");
        }
    }

    public record CustomerProfileResponse(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        VerificationStatus verificationStatus,
        CustomerStatus status,
        boolean canBook) {}

    public record UpdateProfileRequest(String firstName, String lastName, String phone) {}

    public record AddressRequest(
        String label,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank String postalCode,
        @Size(min = 2, max = 2) String country,
        @NotNull Double lat,
        @NotNull Double lng,
        boolean isDefault) {}

    public record AddressResponse(
        UUID id,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        double lat,
        double lng,
        boolean isDefault) {}

    public record AdminCustomerSummary(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        VerificationStatus verificationStatus,
        CustomerStatus status,
        Instant createdAt) {}

    public record AdminCustomerDetail(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiresOn,
        VerificationStatus verificationStatus,
        Instant verificationCompletedAt,
        CustomerStatus status,
        String stripeCustomerId,
        Instant createdAt) {}

    public record UpdateCustomerStatusRequest(@NotNull CustomerStatus status, String reason) {}
}
