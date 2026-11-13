package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Keeps every business NPC in line with what its business owes it.
 * <p>
 * What an NPC is owed comes from its slot's progression in {@code businesses/npcs.yml} and the
 * business's level and tier. This service asks {@link NpcReconciler} to apply the difference
 * whenever that can have changed: when the NPC is created, when its business changes (tier,
 * skill picks), on start, when someone respecs the NPC in SkysongNPCs, and when staff ask for it.
 * It also remembers what was changed on each NPC's sheet.
 * <p>
 * An NPC whose slot the file no longer lists is left exactly as it is. Not thread-safe: use it
 * on the main thread.
 */
public final class NpcProgression implements BusinessChangeListener, BusinessNpcEvents {
    private final NpcSettings settings;
    private final BusinessService businesses;
    private final BusinessNpcs npcs;
    private final NpcReconciler reconciler;
    private final TierOrder tiers;
    private final NpcAppliedRepository repository;
    private final ProgressionNotices notices;
    private final Consumer<String> warnings;
    private final Map<UUID, NpcApplied> applied = new HashMap<>();
    /** NPCs being brought in line right now, so a respec this service causes doesn't start another round. */
    private final Set<UUID> busy = new HashSet<>();

    /**
     * What the service works with.
     *
     * @param businesses every business
     * @param npcs       which NPCs each business has
     * @param sheets     SkysongNPCs and SkysongStats: what each NPC was given
     */
    public record Dependencies(BusinessService businesses, BusinessNpcs npcs, NpcSheetGateway sheets) {
    }

    /**
     * @param tiers    the proficiency tiers, lowest first
     * @param warnings told about NPCs that could not be brought in line, as console lines
     */
    public NpcProgression(NpcSettings settings, Dependencies dependencies, TierOrder tiers, NpcAppliedRepository repository,
                          ProgressionNotices notices, Consumer<String> warnings) {
        this.settings = settings;
        this.businesses = dependencies.businesses();
        this.npcs = dependencies.npcs();
        this.reconciler = new NpcReconciler(dependencies.sheets(), tiers);
        this.tiers = tiers;
        this.repository = repository;
        this.notices = notices;
        this.warnings = warnings;
    }

    /**
     * Loads what was changed on each NPC's sheet and brings every business NPC in line, so config
     * edited while the server was off applies. Call it after {@link BusinessNpcs#loadAll()}.
     *
     * @return how many NPCs changed
     */
    public int loadAll() {
        applied.clear();
        repository.loadAll().forEach((npc, sheet) -> {
            if (npcs.find(npc).isPresent()) {
                applied.put(npc, sheet);
            } else {
                repository.save(npc, NpcApplied.NONE);
            }
        });
        int changed = 0;
        for (Business business : businesses.all()) {
            changed += (int) sync(business).values().stream().filter(Changes::any).count();
        }
        return changed;
    }

    /**
     * @return what the NPC's slot owes it right now; empty when {@code businesses/npcs.yml} no
     *         longer lists the slot's source
     */
    public Optional<Entitlement> owed(Business business, BusinessNpc npc) {
        return settings.entitlement(business, npc.source(), tiers);
    }

    /**
     * Brings every NPC of the business in line.
     *
     * @return what changed for each of its NPCs, oldest first; {@link Changes#NONE} for the ones in line already
     */
    public Map<BusinessNpc, Changes> sync(Business business) {
        Map<BusinessNpc, Changes> changes = new LinkedHashMap<>();
        for (BusinessNpc npc : npcs.of(business)) {
            changes.put(npc, sync(business, npc));
        }
        return changes;
    }

    /**
     * Brings one NPC in line with its business.
     *
     * @return what changed; {@link Changes#NONE} when nothing did, or the NPC could not be changed
     */
    public Changes sync(Business business, BusinessNpc npc) {
        Optional<Entitlement> owed = owed(business, npc);
        if (owed.isEmpty() || !busy.add(npc.npc())) {
            return Changes.NONE;
        }
        try {
            NpcApplied before = applied.getOrDefault(npc.npc(), NpcApplied.NONE);
            NpcReconciler.Result result = reconciler.reconcile(npc.npc(), Map.of(BenefitSources.of(npc.source()), owed.get()),
                    before);
            remember(npc.npc(), before, result.applied());
            return result.changes();
        } catch (RuntimeException e) {
            // One NPC that SkysongNPCs refuses must not stop the others, or the command that changed the business.
            warnings.accept("The NPC " + npc.npc() + " of the business " + business.id()
                    + " could not be given what it is owed: " + e);
            return Changes.NONE;
        } finally {
            busy.remove(npc.npc());
        }
    }

    /**
     * Someone respecced an NPC in SkysongNPCs. For a business NPC this checks it is still in
     * line; a respec this service caused itself is ignored.
     */
    public void respecced(UUID npc) {
        npcs.find(npc).ifPresent(found -> businesses.find(found.businessId()).ifPresent(business -> sync(business, found)));
    }

    /**
     * Staff copied an NPC in SkysongNPCs. The copy of a business NPC is not a business NPC, so
     * the points, picks, choices and abilities the business gave are taken back from it. Baked
     * stats and fixed proficiencies stay on the copy's sheet, as a sheet does not say who set them.
     */
    public void copied(UUID original, UUID copy) {
        npcs.find(original).ifPresent(found -> {
            try {
                reconciler.reconcile(copy, Map.of(BenefitSources.of(found.source()), Entitlement.NONE), NpcApplied.NONE);
            } catch (RuntimeException e) {
                warnings.accept("The copy " + copy + " of the business NPC " + original
                        + " could not have the business's gifts taken back: " + e);
            }
        });
    }

    /** A changed business may owe its NPCs more or less. Runs after {@link BusinessNpcs} freed lost slots. */
    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        sync(business).forEach((npc, changes) -> {
            if (changes.any()) {
                notices.changed(business, npc, changes);
            }
        });
    }

    /** A new NPC receives what it is owed at once, so its first setup already offers it. */
    @Override
    public void created(Business business, BusinessNpc npc) {
        sync(business, npc);
    }

    /** A removed NPC takes its sheet with it; only the memory of it is dropped. */
    @Override
    public void removed(Business business, BusinessNpc npc, RemovalCause cause) {
        if (applied.remove(npc.npc()) != null) {
            repository.save(npc.npc(), NpcApplied.NONE);
        }
    }

    private void remember(UUID npc, NpcApplied before, NpcApplied now) {
        if (now.equals(before)) {
            return;
        }
        if (now.isEmpty()) {
            applied.remove(npc);
        } else {
            applied.put(npc, now);
        }
        repository.save(npc, now);
    }
}
