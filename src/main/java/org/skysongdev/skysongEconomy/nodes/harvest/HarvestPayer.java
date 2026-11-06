package org.skysongdev.skysongEconomy.nodes.harvest;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.NodeException;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Takes what a harvest costs, for random and farm nodes alike. Every part is checked before any
 * is taken. Downtime can't be given back, so it is taken last, and tokens and money are refunded
 * if a later part fails. Not thread-safe: use it on the main thread.
 */
public final class HarvestPayer {
    private final GatheringTokens tokens;
    private final DowntimeWallet downtime;
    private final Wallet wallet;

    public HarvestPayer(GatheringTokens tokens, DowntimeWallet downtime, Wallet wallet) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.downtime = Objects.requireNonNull(downtime, "downtime");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
    }

    /**
     * @throws NodeException {@code node.harvest.no-tokens}, {@code no-money} or {@code no-downtime}
     *                       when the character can't pay; nothing is taken then
     */
    public void pay(CharacterRef character, HarvestCost cost) {
        UUID player = character.player();
        long balance = tokens.balance(character);
        if (balance < cost.harvestTokens()) {
            throw new NodeException("node.harvest.no-tokens", Map.of(
                    "cost", String.valueOf(cost.harvestTokens()), "balance", String.valueOf(balance)));
        }
        if (wallet.balance(player).compareTo(cost.money()) < 0) {
            throw noMoney(cost);
        }
        // trySpend refuses even 0 when the player has no downtime pool, so 0 is never spent.
        if (cost.downtime() > 0 && downtime.available(player) < cost.downtime()) {
            throw noDowntime(cost);
        }
        if (cost.harvestTokens() > 0) {
            tokens.remove(character, cost.harvestTokens());
        }
        if (cost.money().signum() > 0 && !wallet.withdraw(player, cost.money())) {
            refundTokens(character, cost);
            throw noMoney(cost);
        }
        if (cost.downtime() > 0 && !downtime.trySpend(player, cost.downtime())) {
            refundTokens(character, cost);
            if (cost.money().signum() > 0) {
                wallet.deposit(player, cost.money());
            }
            throw noDowntime(cost);
        }
    }

    private NodeException noMoney(HarvestCost cost) {
        return new NodeException("node.harvest.no-money", Map.of("cost", wallet.format(cost.money())));
    }

    private static NodeException noDowntime(HarvestCost cost) {
        return new NodeException("node.harvest.no-downtime", Map.of("cost", String.valueOf(cost.downtime())));
    }

    private void refundTokens(CharacterRef character, HarvestCost cost) {
        if (cost.harvestTokens() > 0) {
            tokens.add(character, cost.harvestTokens());
        }
    }
}
