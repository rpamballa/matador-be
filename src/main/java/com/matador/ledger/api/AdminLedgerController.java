package com.matador.ledger.api;

import com.matador.ledger.LedgerEntryRequest;
import com.matador.ledger.LedgerEntryType;
import com.matador.ledger.LedgerService;
import com.matador.ledger.api.LedgerDtos.AdjustmentRequest;
import com.matador.ledger.api.LedgerDtos.LedgerEntryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ledger")
@Tag(name = "Admin-Ledger")
public class AdminLedgerController {

    private final LedgerService ledgerService;

    public AdminLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/trips/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
    @Operation(summary = "Trip ledger", description = "All ledger entries for a trip.")
    public List<LedgerEntryView> byTrip(@PathVariable UUID tripId) {
        return ledgerService.findByTrip(tripId);
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
    @Operation(summary = "Customer ledger", description = "Paginated ledger history for a customer.")
    public Page<LedgerEntryView> byCustomer(
        @PathVariable UUID customerId, @PageableDefault(size = 50) Pageable pageable) {
        return ledgerService.findByCustomer(customerId, pageable);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manual adjustment", description = "Record a manual ledger adjustment.")
    public Map<String, UUID> adjust(@Valid @RequestBody AdjustmentRequest request) {
        UUID id =
            ledgerService.record(
                new LedgerEntryRequest(
                    null,
                    request.customerId(),
                    request.bookingId(),
                    request.tripId(),
                    null,
                    LedgerEntryType.MANUAL_ADJUSTMENT,
                    request.amountCents(),
                    request.reason(),
                    null,
                    null));
        return Map.of("id", id);
    }
}
