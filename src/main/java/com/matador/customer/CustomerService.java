package com.matador.customer;

import com.matador.customer.api.CustomerDtos.AddressRequest;
import com.matador.customer.api.CustomerDtos.AddressResponse;
import com.matador.customer.api.CustomerDtos.AdminCustomerDetail;
import com.matador.customer.api.CustomerDtos.AdminCustomerSummary;
import com.matador.customer.api.CustomerDtos.CustomerProfileResponse;
import com.matador.customer.api.CustomerDtos.RegisterRequest;
import com.matador.customer.api.CustomerDtos.TokenResponse;
import com.matador.customer.api.CustomerDtos.UpdateProfileRequest;
import com.matador.customer.events.CustomerRegistered;
import com.matador.customer.events.CustomerSuspended;
import com.matador.customer.events.CustomerVerified;
import com.matador.customer.internal.CustomerAddress;
import com.matador.customer.internal.CustomerAddressRepository;
import com.matador.customer.internal.CustomerMapper;
import com.matador.customer.internal.CustomerRepository;
import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.error.UnauthorizedException;
import com.matador.shared.error.ValidationException;
import com.matador.shared.geo.GeoSupport;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.security.jwt.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the customer module. */
@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;
    private final CustomerMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final int minAge;

    public CustomerService(
        CustomerRepository customers,
        CustomerAddressRepository addresses,
        CustomerMapper mapper,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events,
        @Value("${matador.policy.min-customer-age}") int minAge) {
        this.customers = customers;
        this.addresses = addresses;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
        this.minAge = minAge;
    }

    // ---- Authentication ----

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        validatePassword(request.password());
        validateAge(request.dateOfBirth());
        if (customers.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists.");
        }
        if (customers.existsByPhone(request.phone())) {
            throw new ConflictException("PHONE_TAKEN", "An account with this phone already exists.");
        }
        Customer customer =
            Customer.register(
                idGenerator.newId(),
                request.email(),
                request.phone(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.dateOfBirth());
        customers.save(customer);
        events.publishEvent(
            new CustomerRegistered(customer.getId(), customer.getEmail(), clock.instant()));
        return issueTokens(customer);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String rawPassword) {
        Customer customer =
            customers
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        if (customer.getStatus() == CustomerStatus.DEACTIVATED) {
            throw new UnauthorizedException("Account is deactivated.");
        }
        return issueTokens(customer);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        UUID customerId = jwtService.verifyRefreshToken(refreshToken);
        Customer customer =
            customers
                .findById(customerId)
                .orElseThrow(() -> new UnauthorizedException("Unknown customer."));
        return issueTokens(customer);
    }

    private TokenResponse issueTokens(Customer customer) {
        JwtService.TokenPair pair = jwtService.issueTokens(customer.getId(), customer.getEmail());
        return TokenResponse.bearer(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    // ---- Customer self-service ----

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID customerId) {
        return mapper.toProfile(require(customerId));
    }

    @Transactional
    public CustomerProfileResponse updateProfile(UUID customerId, UpdateProfileRequest request) {
        Customer customer = require(customerId);
        if (request.phone() != null
            && !request.phone().equals(customer.getPhone())
            && customers.existsByPhone(request.phone())) {
            throw new ConflictException("PHONE_TAKEN", "An account with this phone already exists.");
        }
        customer.updateProfile(request.firstName(), request.lastName(), request.phone());
        return mapper.toProfile(customer);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID customerId) {
        return addresses.findByCustomerIdOrderByCreatedAtAsc(customerId).stream()
            .map(mapper::toAddressResponse)
            .toList();
    }

    @Transactional
    public AddressResponse addAddress(UUID customerId, AddressRequest request) {
        require(customerId);
        if (request.isDefault()) {
            addresses.clearDefaultFor(customerId);
        }
        CustomerAddress address =
            new CustomerAddress(
                idGenerator.newId(),
                customerId,
                request.label(),
                request.line1(),
                request.line2(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country() == null ? "US" : request.country(),
                GeoSupport.point(request.lng(), request.lat()),
                request.isDefault(),
                clock.instant());
        return mapper.toAddressResponse(addresses.save(address));
    }

    @Transactional
    public void removeAddress(UUID customerId, UUID addressId) {
        CustomerAddress address =
            addresses
                .findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
        addresses.delete(address);
    }

    // ---- Admin operations ----

    @Transactional(readOnly = true)
    public Page<AdminCustomerSummary> search(
        String email,
        String phone,
        VerificationStatus verificationStatus,
        CustomerStatus status,
        Pageable pageable) {
        return customers
            .search(email, phone, verificationStatus, status, pageable)
            .map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminCustomerDetail getDetail(UUID customerId) {
        return mapper.toDetail(require(customerId));
    }

    @Transactional
    public AdminCustomerDetail updateStatus(UUID customerId, CustomerStatus status, String reason) {
        Customer customer = require(customerId);
        switch (status) {
            case SUSPENDED -> {
                customer.suspend();
                events.publishEvent(
                    new CustomerSuspended(customer.getId(), reason, clock.instant()));
            }
            case DEACTIVATED -> customer.deactivate();
            case ACTIVE -> customer.reactivate();
        }
        return mapper.toDetail(customer);
    }

    // ---- Verification lifecycle (driven by identity events) ----

    @Transactional
    public void markVerificationStarted(UUID customerId) {
        Customer customer = require(customerId);
        // Idempotent: a re-published start for an already in-progress customer is a no-op.
        if (customer.getVerificationStatus() == VerificationStatus.IN_PROGRESS) {
            return;
        }
        customer.startVerification();
    }

    @Transactional
    public void applyVerificationResult(
        UUID customerId,
        boolean success,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiresOn,
        Instant completedAt) {
        Customer customer = require(customerId);
        customer.completeVerification(
            success, licenseNumber, licenseState, licenseExpiresOn, completedAt);
        if (success) {
            events.publishEvent(new CustomerVerified(customer.getId(), completedAt));
        }
    }

    /** Used by the daily expiry job. */
    @Transactional(readOnly = true)
    public List<UUID> findVerifiedWithExpiredLicense() {
        return customers
            .findByVerificationStatusAndLicenseExpiresOnBefore(
                VerificationStatus.VERIFIED, LocalDate.now(clock))
            .stream()
            .map(Customer::getId)
            .toList();
    }

    @Transactional
    public void expireVerification(UUID customerId) {
        require(customerId).expireVerification();
    }

    private Customer require(UUID customerId) {
        return customers
            .findById(customerId)
            .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
    }

    private void validatePassword(String password) {
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (password.length() < 12 || !hasLetter || !hasDigit) {
            throw new ValidationException(
                "WEAK_PASSWORD",
                "Password must be at least 12 characters and include a letter and a digit.");
        }
    }

    private void validateAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now(clock)).getYears();
        if (age < minAge) {
            throw new ValidationException(
                "UNDERAGE", "You must be at least %d years old to register.".formatted(minAge));
        }
    }
}
