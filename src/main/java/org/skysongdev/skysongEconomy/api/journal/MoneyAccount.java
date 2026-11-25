package org.skysongdev.skysongEconomy.api.journal;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Where money in a journal record came from or went to.
 *
 * @param type what kind of account
 * @param id   the player's UUID for {@link Type#PLAYER}, the business id for {@link Type#VAULT}, and
 *             {@code server} for {@link Type#SERVER}
 */
public record MoneyAccount(Type type, String id) {
    /** The server: money that enters or leaves the economy. */
    public static final MoneyAccount SERVER = new MoneyAccount(Type.SERVER, "server");

    public MoneyAccount {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    /** What kind of account. New types may be added. */
    public enum Type {
        /** A player's balance. */
        PLAYER,
        /** A business's vault. */
        VAULT,
        /** Outside the economy: money created (such as shift pay) or removed (such as an import's price). */
        SERVER
    }

    /** @return the balance of that player */
    public static MoneyAccount player(UUID player) {
        return new MoneyAccount(Type.PLAYER, player.toString());
    }

    /** @return the vault of that business */
    public static MoneyAccount vault(String businessId) {
        return new MoneyAccount(Type.VAULT, businessId);
    }

    /** @return the player's UUID; empty when it isn't a player's balance */
    public Optional<UUID> playerId() {
        return type == Type.PLAYER ? Optional.of(UUID.fromString(id)) : Optional.empty();
    }
}
