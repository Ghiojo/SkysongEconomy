package org.skysongdev.skysongEconomy.api.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Optional;

/**
 * Reads SkysongEconomy's businesses. Get it with
 * {@code Bukkit.getServicesManager().load(Businesses.class)} and use it on the main thread.
 * Everything it returns is a copy taken when it was called.
 */
public interface Businesses {
    /** @return the business with that id, ignoring case; empty when there is none */
    Optional<BusinessInfo> find(String businessId);

    /** @return every business, in no particular order */
    List<BusinessInfo> all();

    /** @return the businesses the character owns */
    List<BusinessInfo> ownedBy(CharacterRef owner);
}
