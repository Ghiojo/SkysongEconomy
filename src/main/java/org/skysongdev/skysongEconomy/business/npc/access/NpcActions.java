package org.skysongdev.skysongEconomy.business.npc.access;

import org.skysongdev.skysongEconomy.business.npc.NpcAccessGateway;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** SkysongNPCs' action names and presets, for reading what people type and write in config. */
public final class NpcActions {
    private final NpcAccessGateway gateway;

    public NpcActions(NpcAccessGateway gateway) {
        this.gateway = gateway;
    }

    /** @return every action name, in SkysongNPCs' order */
    public Set<String> all() {
        return gateway.actions();
    }

    /**
     * @param written an action name such as {@code inventory-view}, or a preset such as {@code customer}
     * @return the actions it stands for; empty when it is neither
     */
    public Optional<Set<String>> resolve(String written) {
        String name = written.strip().toLowerCase(Locale.ROOT);
        if (gateway.actions().contains(name)) {
            return Optional.of(Set.of(name));
        }
        return gateway.preset(name);
    }
}
