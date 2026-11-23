package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A SkysongClaims Claim as it was when it was read. It does not change when the Claim does; read
 * it again for the latest.
 *
 * @param id          the id staff gave it, in lower case, such as {@code rose_house}
 * @param displayName the name players see, in MiniMessage
 * @param typeId      the id of its SkysongClaims type, such as {@code house}
 * @param free        whether it is a Free Claim, which players take themselves for nothing; those
 *                    are never priced or sold
 * @param owners      its owners, in the order they became owners
 * @param forSaleBy   the name of the plugin that put it on sale, empty when it is not for sale
 */
public record ClaimView(String id, String displayName, String typeId, boolean free, List<CharacterRef> owners,
                        Optional<String> forSaleBy) {
    public ClaimView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(typeId, "typeId");
        owners = List.copyOf(owners);
        Objects.requireNonNull(forSaleBy, "forSaleBy");
    }

    /** @return whether at least one character owns it */
    public boolean isOwned() {
        return !owners.isEmpty();
    }

    /** @return whether the character is one of its owners */
    public boolean isOwnedBy(CharacterRef character) {
        return owners.contains(character);
    }

    /** @return whether it is for sale, by any plugin */
    public boolean isForSale() {
        return forSaleBy.isPresent();
    }

    /**
     * @param plugin a plugin's name, such as {@code SkysongEconomy}
     * @return whether that plugin put it on sale
     */
    public boolean isForSaleBy(String plugin) {
        return forSaleBy.filter(plugin::equals).isPresent();
    }
}
