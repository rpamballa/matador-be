package com.matador.pricing.api;

import com.matador.pricing.PricingService;
import com.matador.pricing.api.PricingDtos.CreatePromoRequest;
import com.matador.pricing.api.PricingDtos.CreateRateRequest;
import com.matador.pricing.api.PricingDtos.PromoResponse;
import com.matador.pricing.api.PricingDtos.RateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pricing")
@Tag(name = "Admin-Settings")
public class AdminPricingController {

    private final PricingService pricingService;

    public AdminPricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/rates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create rate", description = "Add a pricing rate for a vehicle class.")
    public RateResponse createRate(@Valid @RequestBody CreateRateRequest request) {
        return pricingService.createRate(request);
    }

    @PostMapping("/promos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create promo", description = "Create a promo code.")
    public PromoResponse createPromo(@Valid @RequestBody CreatePromoRequest request) {
        return pricingService.createPromo(request);
    }
}
