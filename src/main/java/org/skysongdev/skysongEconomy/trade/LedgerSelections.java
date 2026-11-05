package org.skysongdev.skysongEconomy.trade;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which ledgers each Economy and Logistics business has selected.
 * <p>
 * Only the owner selects and removes ledgers. Selecting is free; removing charges the
 * configured fine to the owner's balance. Deliveries already ordered from a removed ledger still
 * arrive. When a business loses ledger slots (a lower tier, or a cleared pick) its newest
 * selections are dropped without a fine. Ledgers whose file was removed are ignored and take no
 * slot. Kept in memory and saved right away; use it on the main thread.
 */
public final class LedgerSelections implements BusinessChangeListener {
    private final EconomyLogisticsSettings settings;
    private final LedgerRegistry ledgers;
    private final LedgerSelectionRepository repository;
    private final Wallet wallet;
    private final Clock clock;
    private final Map<String, List<SelectedLedger>> byBusiness = new HashMap<>();

    public LedgerSelections(EconomyLogisticsSettings settings, LedgerRegistry ledgers, LedgerSelectionRepository repository,
                            Wallet wallet, Clock clock) {
        this.settings = settings;
        this.ledgers = ledgers;
        this.repository = repository;
        this.wallet = wallet;
        this.clock = clock;
    }

    /** Loads every saved selection. */
    public void loadAll() {
        byBusiness.clear();
        repository.loadAll().stream()
                .sorted(Comparator.comparing(SelectedLedger::selectedAt))
                .forEach(selection -> byBusiness.computeIfAbsent(selection.businessId(), id -> new ArrayList<>()).add(selection));
    }

    /** @return the business's selected ledgers that still exist, oldest selection first */
    public List<Ledger> selected(Business business) {
        return known(business).stream().map(selection -> ledgers.find(selection.ledgerId()).orElseThrow()).toList();
    }

    public boolean isSelected(Business business, String ledgerId) {
        return known(business).stream().anyMatch(selection -> selection.ledgerId().equals(ledgerId));
    }

    /** @return how many more ledgers the business can select */
    public int freeSlots(Business business) {
        return Math.max(0, settings.limits(business).ledgers() - known(business).size());
    }

    /** @return the fine for removing a ledger */
    public BigDecimal removalFine() {
        return settings.ledgerRemovalFine();
    }

    /**
     * Puts a ledger in a free slot.
     *
     * @param by the character doing it, who must own the business
     * @throws BusinessException when they aren't the owner, the business has no Economy and
     *                           Logistics, the ledger is unknown or already selected, or no slot is free
     */
    public Ledger select(Business business, CharacterRef by, String ledgerId) {
        requireOwner(business, by);
        requireEnabled(business);
        Ledger ledger = ledgers.find(ledgerId).orElseThrow(() ->
                new BusinessException("business.ledgers.unknown", Map.of("ledger", ledgerId)));
        if (isSelected(business, ledgerId)) {
            throw new BusinessException("business.ledgers.already-selected", Map.of("ledger", ledgerId));
        }
        if (freeSlots(business) == 0) {
            throw new BusinessException("business.ledgers.full",
                    Map.of("max", String.valueOf(settings.limits(business).ledgers())));
        }
        SelectedLedger selection = new SelectedLedger(business.id(), ledgerId, clock.instant());
        byBusiness.computeIfAbsent(business.id(), id -> new ArrayList<>()).add(selection);
        repository.save(selection);
        return ledger;
    }

    /**
     * Takes a ledger out of its slot and charges the fine to the owner.
     *
     * @return the fine paid
     * @throws BusinessException when they aren't the owner, the ledger isn't selected, or the
     *                           owner can't pay the fine
     */
    public BigDecimal remove(Business business, CharacterRef by, String ledgerId) {
        requireOwner(business, by);
        SelectedLedger selection = find(business, ledgerId).orElseThrow(() ->
                new BusinessException("business.ledgers.not-selected", Map.of("ledger", ledgerId)));
        BigDecimal fine = settings.ledgerRemovalFine();
        if (fine.signum() > 0 && !wallet.withdraw(business.owner().player(), fine)) {
            throw new BusinessException("business.ledgers.cannot-afford-fine", Map.of("fine", wallet.format(fine)));
        }
        drop(selection);
        return fine;
    }

    /** Drops the newest selections the business has no slot for any more. */
    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        List<SelectedLedger> known = known(business);
        int limit = settings.limits(business).ledgers();
        for (int i = known.size() - 1; i >= limit; i--) {
            drop(known.get(i));
        }
    }

    @Override
    public void deleted(Business business) {
        List<SelectedLedger> selections = byBusiness.remove(business.id());
        if (selections != null) {
            selections.forEach(selection -> repository.delete(selection.businessId(), selection.ledgerId()));
        }
    }

    private Optional<SelectedLedger> find(Business business, String ledgerId) {
        return byBusiness.getOrDefault(business.id(), List.of()).stream()
                .filter(selection -> selection.ledgerId().equals(ledgerId))
                .findFirst();
    }

    private List<SelectedLedger> known(Business business) {
        return byBusiness.getOrDefault(business.id(), List.of()).stream()
                .filter(selection -> ledgers.find(selection.ledgerId()).isPresent())
                .toList();
    }

    private void drop(SelectedLedger selection) {
        List<SelectedLedger> selections = byBusiness.get(selection.businessId());
        if (selections != null) {
            selections.remove(selection);
        }
        repository.delete(selection.businessId(), selection.ledgerId());
    }

    private static void requireOwner(Business business, CharacterRef by) {
        if (!business.isOwner(by)) {
            throw new BusinessException("business.not-owner", Map.of("business", business.displayName()));
        }
    }

    private void requireEnabled(Business business) {
        if (!settings.enabled(business)) {
            throw new BusinessException("business.ledgers.not-enabled", Map.of("business", business.displayName()));
        }
    }
}
