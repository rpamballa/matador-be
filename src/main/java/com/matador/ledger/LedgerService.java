package com.matador.ledger;

import com.matador.ledger.api.LedgerDtos.LedgerEntryView;
import com.matador.ledger.internal.LedgerEntry;
import com.matador.ledger.internal.LedgerEntryRepository;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.money.Money;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the ledger module. Entries are insert-only. */
@Service
public class LedgerService {

    private final LedgerEntryRepository entries;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public LedgerService(LedgerEntryRepository entries, IdGenerator idGenerator, Clock clock) {
        this.entries = entries;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public UUID record(LedgerEntryRequest req) {
        LedgerEntry entry =
            new LedgerEntry(
                idGenerator.newId(),
                req.occurredAt() == null ? clock.instant() : req.occurredAt(),
                req.customerId(),
                req.bookingId(),
                req.tripId(),
                req.incidentId(),
                req.entryType(),
                req.amountCents(),
                req.description(),
                req.paymentIntentId(),
                req.metadata(),
                clock.instant());
        entries.save(entry);
        return entry.getId();
    }

    @Transactional(readOnly = true)
    public Money customerOutstandingBalance(UUID customerId) {
        return Money.usd(entries.sumByCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public Money tripNetCharges(UUID tripId) {
        return Money.usd(entries.sumByTrip(tripId));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryView> findByTrip(UUID tripId) {
        return entries.findByTripIdOrderByOccurredAtAsc(tripId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryView> findByCustomer(UUID customerId, Pageable pageable) {
        return entries.findByCustomerIdOrderByOccurredAtDesc(customerId, pageable).map(this::toView);
    }

    private LedgerEntryView toView(LedgerEntry e) {
        return new LedgerEntryView(
            e.getId(), e.getOccurredAt(), e.getEntryType(), e.getAmountCents(), e.getCurrency(), e.getDescription());
    }
}
