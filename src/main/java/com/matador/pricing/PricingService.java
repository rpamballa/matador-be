package com.matador.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matador.pricing.api.PricingDtos.CreatePromoRequest;
import com.matador.pricing.api.PricingDtos.CreateRateRequest;
import com.matador.pricing.api.PricingDtos.PromoResponse;
import com.matador.pricing.api.PricingDtos.RateResponse;
import com.matador.pricing.internal.PricingQuoteEntity;
import com.matador.pricing.internal.PricingQuoteRepository;
import com.matador.pricing.internal.PricingRate;
import com.matador.pricing.internal.PricingRateRepository;
import com.matador.pricing.internal.PromoCode;
import com.matador.pricing.internal.PromoCodeRepository;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.error.ValidationException;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.security.CurrentUser;
import com.matador.vehicle.VehicleClassInfo;
import com.matador.vehicle.VehicleService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Public API for the pricing module. */
@Service
public class PricingService {

    private final PricingQuoteRepository quotes;
    private final PricingRateRepository rates;
    private final PromoCodeRepository promos;
    private final VehicleService vehicleService;
    private final PricingProperties properties;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public PricingService(
        PricingQuoteRepository quotes,
        PricingRateRepository rates,
        PromoCodeRepository promos,
        VehicleService vehicleService,
        PricingProperties properties,
        ObjectMapper objectMapper,
        IdGenerator idGenerator,
        Clock clock) {
        this.quotes = quotes;
        this.rates = rates;
        this.promos = promos;
        this.vehicleService = vehicleService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public QuoteInfo createQuote(QuoteRequest request) {
        VehicleClassInfo vehicleClass = vehicleService.requireClass(request.vehicleClassId());
        Instant now = clock.instant();
        String tier = request.insuranceTierOrDefault();

        Optional<PricingRate> rate =
            rates
                .findActiveRate(request.vehicleClassId(), tier, now, Limit.of(1))
                .stream()
                .findFirst();

        long dailyRate = rate.map(PricingRate::getDailyRateCents).orElse(vehicleClass.baseDailyRateCents());
        long deliveryFee = rate.map(PricingRate::getDeliveryFeeCents).orElse(0L);
        long insuranceDaily = rate.map(PricingRate::getInsuranceDailyCents).orElse(0L);

        int days = PricingCalculator.billableDays(request.pickupAt(), request.dropoffAt());
        long outOfZone = request.dropoffInZone() ? 0 : request.outOfZoneFeeCents();
        long grossSubtotal = dailyRate * days + deliveryFee + insuranceDaily * days + outOfZone;

        long discount = 0;
        String promoLabel = null;
        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            PromoCode promo =
                promos
                    .findByCode(request.promoCode())
                    .filter(p -> p.isValid(now))
                    .orElseThrow(
                        () -> new ValidationException("INVALID_PROMO", "Promo code is not valid."));
            discount = promo.discountFor(grossSubtotal);
            promoLabel = promo.getCode();
        }

        QuoteComputation computation =
            PricingCalculator.calculate(
                new PricingCalculator.Inputs(
                    request.pickupAt(),
                    request.dropoffAt(),
                    dailyRate,
                    deliveryFee,
                    insuranceDaily,
                    vehicleClass.name(),
                    request.dropoffInZone(),
                    request.outOfZoneFeeCents(),
                    discount,
                    promoLabel,
                    properties.taxRateBps(),
                    properties.depositMinCents(),
                    properties.depositPercentBps()));

        Instant expiresAt = now.plus(properties.quoteTtlMinutes(), ChronoUnit.MINUTES);
        PricingQuoteEntity entity =
            new PricingQuoteEntity(
                idGenerator.newId(),
                request.customerId(),
                request.vehicleClassId(),
                request.pickupAt(),
                request.dropoffAt(),
                request.dropoffInZone(),
                tier,
                writeLineItems(computation.lineItems()),
                computation.subtotalCents(),
                computation.taxCents(),
                computation.totalCents(),
                computation.depositCents(),
                promoLabel,
                computation.discountCents(),
                expiresAt,
                now);
        quotes.save(entity);
        return toInfo(entity, computation.lineItems());
    }

    @Transactional(readOnly = true)
    public QuoteInfo getQuote(UUID quoteId) {
        PricingQuoteEntity q = requireQuote(quoteId);
        return toInfo(q, readLineItems(q.getLineItems()));
    }

    @Transactional(readOnly = true)
    public boolean isQuoteValid(UUID quoteId) {
        return quotes
            .findById(quoteId)
            .map(q -> q.getExpiresAt().isAfter(clock.instant()))
            .orElse(false);
    }

    /** Increments the promo usage counter when a booking using this quote is confirmed. */
    @Transactional
    public void consumePromoForQuote(UUID quoteId) {
        PricingQuoteEntity q = requireQuote(quoteId);
        if (q.getPromoCode() != null) {
            promos.findByCode(q.getPromoCode()).ifPresent(PromoCode::incrementUse);
        }
    }

    // ---- Admin ----

    @Transactional
    public RateResponse createRate(CreateRateRequest req) {
        PricingRate rate =
            new PricingRate(
                idGenerator.newId(),
                req.vehicleClassId(),
                req.dailyRateCents(),
                req.deliveryFeeCents(),
                req.outOfZoneDropoffFeeCents(),
                req.insuranceTier(),
                req.insuranceDailyCents(),
                req.effectiveFrom() == null ? clock.instant() : req.effectiveFrom(),
                clock.instant(),
                CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID));
        rates.save(rate);
        return new RateResponse(rate.getId(), rate.getVehicleClassId(), rate.getDailyRateCents());
    }

    @Transactional
    public PromoResponse createPromo(CreatePromoRequest req) {
        if (promos.findByCode(req.code()).isPresent()) {
            throw new ValidationException("PROMO_EXISTS", "Promo code already exists.");
        }
        PromoCode promo =
            new PromoCode(
                idGenerator.newId(),
                req.code(),
                req.discountType(),
                req.discountValue(),
                req.maxUses(),
                req.startsAt() == null ? clock.instant() : req.startsAt(),
                req.expiresAt(),
                clock.instant());
        promos.save(promo);
        return new PromoResponse(promo.getId(), req.code(), req.discountType(), req.discountValue());
    }

    // ---- helpers ----

    private PricingQuoteEntity requireQuote(UUID quoteId) {
        return quotes.findById(quoteId).orElseThrow(() -> ResourceNotFoundException.of("Quote", quoteId));
    }

    private QuoteInfo toInfo(PricingQuoteEntity q, List<LineItem> lineItems) {
        return new QuoteInfo(
            q.getId(),
            q.getVehicleClassId(),
            q.getPickupAt(),
            q.getDropoffAt(),
            q.isDropoffInZone(),
            q.getInsuranceTier(),
            lineItems,
            q.getSubtotalCents(),
            q.getDiscountCents(),
            q.getTaxCents(),
            q.getTotalCents(),
            q.getDepositCents(),
            q.getExpiresAt());
    }

    private String writeLineItems(List<LineItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize line items", e);
        }
    }

    private List<LineItem> readLineItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<LineItem>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read line items", e);
        }
    }
}
