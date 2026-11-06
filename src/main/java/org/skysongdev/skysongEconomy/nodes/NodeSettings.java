package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.business.display.NameplateSettings;

import java.util.Objects;
import java.util.Set;

/**
 * Server-wide node settings, from {@code nodes/settings.yml}.
 *
 * @param ownerBusinessTypes the business types that may own nodes
 * @param nameplate          how the floating name above node blocks looks
 */
public record NodeSettings(Set<String> ownerBusinessTypes, NameplateSettings nameplate) {
    public NodeSettings {
        ownerBusinessTypes = Set.copyOf(ownerBusinessTypes);
        Objects.requireNonNull(nameplate, "nameplate");
    }
}
