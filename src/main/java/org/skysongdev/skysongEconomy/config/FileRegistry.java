package org.skysongdev.skysongEconomy.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every entry that loaded from one folder, such as {@code nodes/random}, by id. A reload builds a
 * new registry instead of changing this one.
 *
 * @param <T> what each file holds
 */
public final class FileRegistry<T extends FileEntry> {
    private final Map<String, T> entries;

    /** @param entries the entries in file name order; a later one with the same id replaces an earlier one */
    public FileRegistry(Collection<? extends T> entries) {
        Map<String, T> byId = new LinkedHashMap<>();
        entries.forEach(entry -> byId.put(entry.id(), entry));
        this.entries = Collections.unmodifiableMap(byId);
    }

    /** @return a registry with nothing in it */
    public static <T extends FileEntry> FileRegistry<T> empty() {
        return new FileRegistry<>(List.of());
    }

    /** @return the entry with that id, empty when its file is missing or didn't load */
    public Optional<T> find(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    /** @return every entry, in file name order */
    public Collection<T> all() {
        return List.copyOf(entries.values());
    }

    /** @return every id, in file name order */
    public Set<String> ids() {
        return entries.keySet();
    }

    public int size() {
        return entries.size();
    }
}
