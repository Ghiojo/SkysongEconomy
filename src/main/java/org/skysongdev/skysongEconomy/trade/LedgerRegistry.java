package org.skysongdev.skysongEconomy.trade;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every ledger the server offers, by id, in name order of their files. */
public final class LedgerRegistry {
    private final Map<String, Ledger> byId;

    public LedgerRegistry(List<Ledger> ledgers) {
        Map<String, Ledger> map = new LinkedHashMap<>();
        for (Ledger ledger : ledgers) {
            if (map.putIfAbsent(ledger.id(), ledger) != null) {
                throw new IllegalArgumentException("Two ledgers have the id " + ledger.id());
            }
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public Optional<Ledger> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<Ledger> all() {
        return byId.values();
    }

    public int size() {
        return byId.size();
    }
}
