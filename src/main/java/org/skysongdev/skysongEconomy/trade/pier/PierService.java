package org.skysongdev.skysongEconomy.trade.pier;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.api.piers.PierException;
import org.skysongdev.skysongEconomy.api.piers.Piers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Every receiving pier: the ones in {@code piers.yml}, merged with what was changed on them through
 * the API or commands, and the ones plugins created. Kept in memory and saved through a
 * {@link PierRepository} at every change. Main thread only.
 * <p>
 * If a plugin pier and a pier in the file share an id, the plugin pier wins and the file's is
 * ignored with a warning, so a config edit never takes a pier away from the plugin that made it.
 * Changes saved for a pier that was later deleted from the file are kept, hidden, so putting the
 * pier back in the file brings them back.
 */
public final class PierService implements Piers {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PierRepository repository;
    private final Consumer<String> warnings;
    private final List<PierChangeListener> listeners = new ArrayList<>();
    private PierSettings settings;
    /** Saved rows by pier id, in the order they were loaded or created. */
    private final Map<String, PierRepository.Row> rows = new LinkedHashMap<>();
    /** Modifiers set through the API or commands, by pier id, then modifier id. */
    private final Map<String, Map<String, LossModifier>> modifiers = new HashMap<>();

    /**
     * @param settings what {@code piers.yml} holds
     * @param warnings told when a pier in the file is ignored because a plugin pier has its id
     */
    public PierService(PierSettings settings, PierRepository repository, Consumer<String> warnings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** Loads every saved pier and modifier, replacing what is in memory. */
    public void loadAll() {
        rows.clear();
        modifiers.clear();
        repository.loadPiers().forEach(row -> rows.put(row.id(), row));
        repository.loadModifiers().forEach(row -> modifiers.computeIfAbsent(row.pierId(), id -> new LinkedHashMap<>())
                .put(row.modifier().id(), row.modifier()));
        warnAboutClashes();
    }

    /** Tells {@code listener} about every later change, including reloads. */
    public void addListener(PierChangeListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Puts newly read {@code piers.yml} settings in use and tells the listeners about every pier
     * that changed, appeared or disappeared.
     */
    public void reload(PierSettings newSettings) {
        Map<String, Pier> before = byId();
        settings = Objects.requireNonNull(newSettings, "newSettings");
        warnAboutClashes();
        Map<String, Pier> after = byId();
        Set<String> ids = new LinkedHashSet<>(before.keySet());
        ids.addAll(after.keySet());
        for (String id : ids) {
            fire(Optional.ofNullable(before.get(id)), Optional.ofNullable(after.get(id)));
        }
    }

    @Override
    public List<Pier> all() {
        List<Pier> piers = new ArrayList<>();
        for (String id : settings.piers().keySet()) {
            if (!isPluginPier(id)) {
                piers.add(view(id).orElseThrow());
            }
        }
        for (PierRepository.Row row : rows.values()) {
            if (!row.source().equals(Pier.CONFIG)) {
                piers.add(view(row.id()).orElseThrow());
            }
        }
        return List.copyOf(piers);
    }

    @Override
    public Optional<Pier> find(String id) {
        return id == null ? Optional.empty() : view(id.strip().toLowerCase(Locale.ROOT));
    }

    @Override
    public Pier create(String owner, String id, PierDetails details) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(details, "details");
        String pierId = Piers.checkId(id, "pier id");
        if (owner.isBlank() || owner.equalsIgnoreCase(Pier.CONFIG)) {
            throw new PierException("piers.invalid-owner", Map.of("owner", owner));
        }
        if (view(pierId).isPresent() || settings.piers().containsKey(pierId)) {
            throw PierException.about("piers.exists", pierId);
        }
        // A pier from the file that was deleted may have left a hidden row; a new pier replaces it.
        modifiers.remove(pierId);
        repository.deletePier(pierId);
        save(new PierRepository.Row(pierId, owner, details, true, Map.of(), BigDecimal.ZERO));
        Pier created = view(pierId).orElseThrow();
        fire(Optional.empty(), Optional.of(created));
        return created;
    }

    @Override
    public Pier update(String id, PierDetails details) {
        Objects.requireNonNull(details, "details");
        PierRepository.Row row = pluginRow(id);
        return change(row.id(), () -> save(new PierRepository.Row(row.id(), row.source(), details, row.open(), row.data(),
                row.lossChance())));
    }

    @Override
    public void remove(String id) {
        PierRepository.Row row = pluginRow(id);
        Pier before = view(row.id()).orElseThrow();
        rows.remove(row.id());
        modifiers.remove(row.id());
        repository.deletePier(row.id());
        fire(Optional.of(before), Optional.empty());
    }

    @Override
    public Pier setOpen(String id, boolean open) {
        PierRepository.Row row = rowFor(id);
        return change(row.id(), () -> save(new PierRepository.Row(row.id(), row.source(), row.details(), open, row.data(),
                row.lossChance())));
    }

    @Override
    public Pier setData(String id, String key, @Nullable String value) {
        Objects.requireNonNull(key, "key");
        PierRepository.Row row = rowFor(id);
        Map<String, String> data = new LinkedHashMap<>(row.data());
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        return change(row.id(), () -> save(new PierRepository.Row(row.id(), row.source(), row.details(), row.open(), data,
                row.lossChance())));
    }

    @Override
    public Pier setLossChance(String id, BigDecimal percent) {
        Objects.requireNonNull(percent, "percent");
        if (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new PierException("piers.invalid-chance", Map.of("chance", percent.toPlainString()));
        }
        PierRepository.Row row = rowFor(id);
        return change(row.id(), () -> save(new PierRepository.Row(row.id(), row.source(), row.details(), row.open(),
                row.data(), percent)));
    }

    @Override
    public Pier setLossModifier(String id, LossModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        String pierId = existing(id);
        return change(pierId, () -> {
            modifiers.computeIfAbsent(pierId, key -> new LinkedHashMap<>()).put(modifier.id(), modifier);
            repository.saveModifier(new PierRepository.ModifierRow(pierId, modifier));
        });
    }

    @Override
    public boolean removeLossModifier(String id, String modifierId) {
        String pierId = existing(id);
        String key = Piers.checkId(modifierId, "loss modifier id");
        Map<String, LossModifier> set = modifiers.getOrDefault(pierId, Map.of());
        if (!set.containsKey(key)) {
            ConfiguredPier configured = fileView(pierId);
            if (configured != null && configured.lossModifiers().containsKey(key)) {
                throw new PierException("piers.modifier-from-config", Map.of("pier", pierId, "modifier", key));
            }
            return false;
        }
        change(pierId, () -> {
            set.remove(key);
            repository.deleteModifier(pierId, key);
        });
        return true;
    }

    /** Runs a change to an existing pier, then tells the listeners. */
    private Pier change(String pierId, Runnable change) {
        Pier before = view(pierId).orElseThrow();
        change.run();
        Pier after = view(pierId).orElseThrow();
        fire(Optional.of(before), Optional.of(after));
        return after;
    }

    private void save(PierRepository.Row row) {
        rows.put(row.id(), row);
        repository.savePier(row);
    }

    /** @return the pier's saved row, or a fresh one for a pier from the file with nothing changed yet */
    private PierRepository.Row rowFor(String id) {
        String pierId = existing(id);
        PierRepository.Row row = rows.get(pierId);
        return row != null ? row : new PierRepository.Row(pierId, Pier.CONFIG, null, true, Map.of(), null);
    }

    /** @throws PierException when it is no pier, or one from the file */
    private PierRepository.Row pluginRow(String id) {
        String pierId = existing(id);
        if (!isPluginPier(pierId)) {
            throw PierException.about("piers.from-config", pierId);
        }
        return rows.get(pierId);
    }

    /** @return the normalized id of a pier that exists */
    private String existing(String id) {
        return find(id).map(Pier::id).orElseThrow(() -> PierException.about("piers.unknown", String.valueOf(id)));
    }

    private boolean isPluginPier(String id) {
        PierRepository.Row row = rows.get(id);
        return row != null && !row.source().equals(Pier.CONFIG);
    }

    /** @return the file's pier with that id, unless a plugin pier has it */
    private @Nullable ConfiguredPier fileView(String id) {
        return isPluginPier(id) ? null : settings.piers().get(id);
    }

    private Optional<Pier> view(String id) {
        PierRepository.Row row = rows.get(id);
        Map<String, LossModifier> set = modifiers.getOrDefault(id, Map.of());
        if (isPluginPier(id)) {
            return Optional.of(new Pier(id, Objects.requireNonNull(row.details(), "plugin pier details"), row.open(),
                    row.data(), Objects.requireNonNullElse(row.lossChance(), BigDecimal.ZERO), List.copyOf(set.values()),
                    row.source()));
        }
        ConfiguredPier configured = settings.piers().get(id);
        if (configured == null) {
            return Optional.empty();
        }
        Map<String, String> data = new LinkedHashMap<>(configured.data());
        Map<String, LossModifier> merged = new LinkedHashMap<>(configured.lossModifiers());
        merged.putAll(set);
        BigDecimal chance = configured.lossChance();
        boolean open = true;
        if (row != null) {
            data.putAll(row.data());
            open = row.open();
            chance = Objects.requireNonNullElse(row.lossChance(), chance);
        }
        return Optional.of(new Pier(id, configured.details(), open, data, chance, List.copyOf(merged.values()), Pier.CONFIG));
    }

    private Map<String, Pier> byId() {
        Map<String, Pier> piers = new LinkedHashMap<>();
        all().forEach(pier -> piers.put(pier.id(), pier));
        return piers;
    }

    private void warnAboutClashes() {
        for (String id : settings.piers().keySet()) {
            if (isPluginPier(id)) {
                warnings.accept("The pier " + id + " in " + PierSettingsParser.FILE + " is ignored: the plugin "
                        + rows.get(id).source() + " already made a pier with that id");
            }
        }
    }

    private void fire(Optional<Pier> before, Optional<Pier> after) {
        if (before.equals(after)) {
            return;
        }
        listeners.forEach(listener -> listener.changed(before, after));
    }
}
