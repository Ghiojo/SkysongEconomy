package org.skysongdev.skysongEconomy.business.module;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents.RemovalCause;
import org.skysongdev.skysongEconomy.business.module.ModuleOption.Blocked;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The modules each business picked.
 * <p>
 * A skill listed in {@code businesses/modules.yml} gives a business module picks; each pick
 * takes one module from that skill's pool and is permanent, like a skill pick. This service
 * knows which business picked what, checks a module's {@code max-picks} and {@code requires}
 * when it is picked, and drops the newest picks of a skill that gives fewer than it has (a lower
 * tier, a cleared skill pick, an edited file).
 * <p>
 * Picks of a skill the file no longer lists, and picks of a module whose file is gone, are left
 * alone, so a typo never takes a module from anyone; staff clear them when that was meant. Not
 * thread-safe: use it on the main thread.
 */
public final class BusinessModules implements BusinessChangeListener {
    private final ModuleSettings settings;
    private final BusinessService businesses;
    private final ModulePickRepository repository;
    /** Told about picked and removed modules, in the order they were added. */
    private final List<BusinessModuleEvents> listeners = new ArrayList<>();
    private final Clock clock;
    private final Consumer<String> warnings;
    /** Every pick by business id, oldest first. */
    private final Map<String, List<ModulePick>> byBusiness = new HashMap<>();
    /** Picks dropped because their skill gives fewer now, by business id, until someone reports them. */
    private final Map<String, List<ModulePick>> lostPicks = new HashMap<>();
    /** Businesses whose picks are being brought in line right now. */
    private final Set<String> syncing = new HashSet<>();

    /** @param warnings told about saved picks that could not be kept or no longer match the files, as console lines */
    public BusinessModules(ModuleSettings settings, BusinessService businesses, ModulePickRepository repository,
                           BusinessModuleEvents events, Clock clock, Consumer<String> warnings) {
        this.settings = settings;
        this.businesses = businesses;
        this.repository = repository;
        this.listeners.add(events);
        this.clock = clock;
        this.warnings = warnings;
    }

    /**
     * Loads every module pick and brings them in line with their businesses. Rows whose business
     * is gone are forgotten. Call it after the businesses are loaded.
     *
     * @return how many module picks there are
     */
    public int loadAll() {
        byBusiness.clear();
        lostPicks.clear();
        List<ModulePick> saved = new ArrayList<>(repository.loadAll());
        saved.sort(Comparator.comparingInt(ModulePick::order));
        for (ModulePick pick : saved) {
            if (businesses.find(pick.businessId()).isEmpty()) {
                warnings.accept("The business " + pick.businessId() + " no longer exists, so its module "
                        + pick.moduleId() + " was forgotten");
                repository.delete(pick.businessId(), pick.order());
                continue;
            }
            byBusiness.computeIfAbsent(pick.businessId(), id -> new ArrayList<>()).add(pick);
        }
        for (Business business : businesses.all()) {
            for (ModulePick pick : of(business)) {
                if (!settings.skills().has(pick.skill())) {
                    warnings.accept(ModuleSettingsParser.FILE + " no longer gives " + business.id() + " module picks for "
                            + pick.skill() + ", so its module " + pick.moduleId() + " is kept as it is");
                } else if (settings.modules().find(pick.moduleId()).isEmpty()) {
                    warnings.accept("The module " + pick.moduleId() + " of " + business.id() + " has no file in "
                            + ModuleParser.FOLDER + " any more; the pick is kept");
                }
            }
            sync(business);
        }
        lostPicks.clear();
        return byBusiness.values().stream().mapToInt(List::size).sum();
    }

    /** Adds something to tell about every module a business picks or loses, after the ones added before. */
    public void addListener(BusinessModuleEvents listener) {
        listeners.add(listener);
    }

    /** @return the module with that id, empty when it has no file */
    public Optional<Module> module(String moduleId) {
        return settings.modules().find(moduleId);
    }

    /** @return every module that has a file, in file name order */
    public Collection<Module> modules() {
        return settings.modules().all();
    }

    /** @return every module the business picked, oldest first */
    public List<ModulePick> of(Business business) {
        return List.copyOf(byBusiness.getOrDefault(business.id(), List.of()));
    }

    /** @return whether the business has that module, picked for any skill */
    public boolean has(Business business, String moduleId) {
        return count(business, moduleId) > 0;
    }

    /**
     * @return the business's module picks by skill, in file order: every skill that gives it
     *         picks now, then any skill that only still has picked modules
     */
    public List<ModulePickGroup> groups(Business business) {
        Map<String, List<ModulePick>> picked = new LinkedHashMap<>();
        settings.totals(business).keySet().forEach(skill -> picked.put(skill, new ArrayList<>()));
        of(business).forEach(pick -> picked.computeIfAbsent(pick.skill(), skill -> new ArrayList<>()).add(pick));
        List<ModulePickGroup> groups = new ArrayList<>();
        picked.forEach((skill, picks) -> {
            int total = settings.total(business, skill).orElse(picks.size());
            if (total > 0 || !picks.isEmpty()) {
                groups.add(new ModulePickGroup(skill, total, picks));
            }
        });
        return groups;
    }

    /** @return whether the business has any module pick or module, so its modules are worth showing */
    public boolean hasPicks(Business business) {
        return !groups(business).isEmpty();
    }

    /** @return how many module picks of the skill are still free */
    public int free(Business business, String skill) {
        return groups(business).stream().filter(group -> group.skill().equals(skill)).mapToInt(ModulePickGroup::free)
                .findFirst().orElse(0);
    }

    /** @return the skill's pool as the business sees it, in file order, with why each module can't be picked */
    public List<ModuleOption> pool(Business business, String skill) {
        return settings.pool(skill).stream().map(module -> new ModuleOption(module, blocked(business, skill, module))).toList();
    }

    /**
     * Picks a module for one of the skill's free picks. The pick is permanent.
     *
     * @throws BusinessException when the skill has no free pick, the module is not in the skill's
     *                           pool, or its {@code max-picks} or {@code requires} refuse it
     */
    public ModulePick pick(Business business, String skill, String moduleId) {
        if (free(business, skill) == 0) {
            throw new BusinessException("business.modules.no-free-pick", Map.of("skill", skill));
        }
        Module module = settings.pool(skill).stream().filter(candidate -> candidate.id().equals(moduleId)).findFirst()
                .orElseThrow(() -> new BusinessException("business.modules.not-in-pool", Map.of("module", moduleId, "skill", skill)));
        Blocked blocked = blocked(business, skill, module);
        if (blocked != null) {
            throw new BusinessException(blocked.messageKey(), blockedPlaceholders(module));
        }
        List<ModulePick> picks = byBusiness.computeIfAbsent(business.id(), id -> new ArrayList<>());
        int order = picks.stream().mapToInt(ModulePick::order).max().orElse(0) + 1;
        ModulePick pick = new ModulePick(business.id(), order, skill, module.id(), clock.instant());
        picks.add(pick);
        repository.save(pick);
        listeners.forEach(listener -> listener.picked(business, pick));
        return pick;
    }

    /**
     * Staff only: frees one of the skill's module picks.
     *
     * @param number which of the skill's modules, counted from 1, oldest first
     * @return the removed pick
     * @throws BusinessException {@code business.modules.no-such-pick} when the skill has no module with that number
     */
    public ModulePick clear(Business business, String skill, int number) {
        List<ModulePick> ofSkill = of(business).stream().filter(pick -> pick.skill().equals(skill)).toList();
        if (number < 1 || number > ofSkill.size()) {
            throw new BusinessException("business.modules.no-such-pick", Map.of("number", String.valueOf(number),
                    "skill", skill, "count", String.valueOf(ofSkill.size())));
        }
        ModulePick pick = ofSkill.get(number - 1);
        remove(business, pick, RemovalCause.CLEARED);
        return pick;
    }

    /**
     * Brings the business's modules in line with it: drops the newest picks of every skill that
     * gives fewer picks than it has modules.
     *
     * @return the dropped picks, newest first
     */
    public List<ModulePick> sync(Business business) {
        List<ModulePick> removed = new ArrayList<>();
        // Removing a module can change the business again (its extra workers go), which asks for
        // another sync while this one is still dropping picks; this one finishes the job.
        if (!syncing.add(business.id())) {
            return removed;
        }
        try {
            dropOverTotal(business, removed);
        } finally {
            syncing.remove(business.id());
        }
        return removed;
    }

    private void dropOverTotal(Business business, List<ModulePick> removed) {
        for (ModulePickGroup group : groups(business)) {
            OptionalInt total = settings.total(business, group.skill());
            if (total.isEmpty()) {
                continue;
            }
            List<ModulePick> picks = group.picks();
            for (int i = picks.size() - 1; i >= total.getAsInt(); i--) {
                remove(business, picks.get(i), RemovalCause.PICK_LOST);
                removed.add(picks.get(i));
            }
        }
    }

    /**
     * @return the modules the business lost with their picks since this was last asked, newest
     *         first, so the command that caused it can say so
     */
    public List<ModulePick> takeLostPicks(Business business) {
        List<ModulePick> lost = lostPicks.remove(business.id());
        return lost == null ? List.of() : lost;
    }

    /** A changed business may have fewer module picks. */
    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        List<ModulePick> removed = sync(business);
        if (!removed.isEmpty()) {
            lostPicks.computeIfAbsent(business.id(), id -> new ArrayList<>()).addAll(0, removed);
        }
    }

    /** A deleted business's modules go with it; the database drops their rows itself. */
    @Override
    public void deleted(Business business) {
        lostPicks.remove(business.id());
        List<ModulePick> picks = byBusiness.remove(business.id());
        if (picks != null) {
            picks.forEach(pick -> listeners.forEach(listener -> listener.removed(business, pick, RemovalCause.BUSINESS_DELETED)));
        }
    }

    private void remove(Business business, ModulePick pick, RemovalCause cause) {
        List<ModulePick> picks = byBusiness.get(business.id());
        if (picks != null) {
            picks.remove(pick);
        }
        repository.delete(pick.businessId(), pick.order());
        listeners.forEach(listener -> listener.removed(business, pick, cause));
    }

    private int count(Business business, String moduleId) {
        return (int) byBusiness.getOrDefault(business.id(), List.of()).stream()
                .filter(pick -> pick.moduleId().equals(moduleId)).count();
    }

    /** @return why the business can't pick the module for that skill now, {@code null} when it can */
    private @Nullable Blocked blocked(Business business, String skill, Module module) {
        ModuleRequirements requires = module.requires();
        if (count(business, module.id()) >= module.maxPicks()) {
            return Blocked.MAX_PICKS;
        }
        if (business.tier() < requires.tier()) {
            return Blocked.TIER;
        }
        if (business.pickCount(skill) < requires.level()) {
            return Blocked.LEVEL;
        }
        if (!requires.modules().stream().allMatch(required -> has(business, required))) {
            return Blocked.MODULES;
        }
        return null;
    }

    /** @return the placeholders every {@link Blocked} message may use */
    public static Map<String, String> blockedPlaceholders(Module module) {
        return Map.of("max", String.valueOf(module.maxPicks()), "tier", String.valueOf(module.requires().tier()),
                "level", String.valueOf(module.requires().level()), "modules", String.join(", ", module.requires().modules()));
    }
}
