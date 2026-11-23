package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.money.Wallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Sellers' shares of Claim sales that couldn't be paid at once, kept in memory and saved, and paid
 * when the seller next joins. Main thread only.
 */
public final class ClaimPayouts {
    private final ClaimPayoutRepository repository;
    private final Wallet wallet;
    private final Map<UUID, List<PendingPayout>> byAccount = new HashMap<>();

    public ClaimPayouts(ClaimPayoutRepository repository, Wallet wallet) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
    }

    /** Loads every payout still owed. Call once at start-up. */
    public void loadAll() {
        byAccount.clear();
        repository.loadAll().forEach(this::remember);
    }

    /** @return what the player is still owed, oldest first */
    public List<PendingPayout> owed(UUID account) {
        return List.copyOf(byAccount.getOrDefault(account, List.of()));
    }

    /** Keeps a share to pay later. */
    void owe(PendingPayout payout) {
        remember(payout);
        repository.save(payout);
    }

    /**
     * Pays the player everything they are owed that the wallet accepts now; the rest keeps waiting.
     *
     * @return the payouts paid
     */
    public List<PendingPayout> payOut(UUID account) {
        List<PendingPayout> paid = new ArrayList<>();
        for (PendingPayout payout : owed(account)) {
            if (wallet.deposit(account, payout.amount())) {
                byAccount.get(account).remove(payout);
                repository.delete(payout.saleId(), account);
                paid.add(payout);
            }
        }
        if (byAccount.containsKey(account) && byAccount.get(account).isEmpty()) {
            byAccount.remove(account);
        }
        return paid;
    }

    private void remember(PendingPayout payout) {
        byAccount.computeIfAbsent(payout.account(), account -> new ArrayList<>()).add(payout);
    }
}
