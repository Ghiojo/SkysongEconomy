package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents.RemovalCause;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The NPCs each business has, and the slots they fill.
 * <p>
 * A skill listed in {@code businesses/npcs.yml} gives a business NPC slots; each slot can hold
 * one SkysongNPC created from that skill's template. This service knows which NPC fills which
 * slot, creates and removes them through {@link NpcGateway}, and keeps them in line with the
 * business:
 * <ul>
 *   <li>when a source gives fewer slots than it has NPCs (a lower tier, a cleared skill pick, an
 *       edited file), the newest NPCs over the limit are removed;</li>
 *   <li>every NPC is owned by the business owner's character, or by the server for a
 *       server-owned business, and follows the business when it changes hands;</li>
 *   <li>a deleted business's NPCs are removed with it.</li>
 * </ul>
 * NPCs of a source the file no longer lists are left alone, so a typo in the file never deletes
 * anyone's NPC; staff dismiss them when that was meant. Not thread-safe: use it on the main
 * thread.
 */
public final class BusinessNpcs implements BusinessChangeListener {
    private final NpcSettings settings;
    private final BusinessService businesses;
    private final AccessService access;
    private final NpcGateway gateway;
    private final BusinessNpcRepository repository;
    /** Told about created and removed NPCs, in the order they were added. */
    private final List<BusinessNpcEvents> listeners = new ArrayList<>();
    private final Clock clock;
    private final Consumer<String> warnings;
    /** Every business NPC by NPC id, in the order they were created. */
    private final Map<UUID, BusinessNpc> byNpc = new LinkedHashMap<>();
    /** NPCs removed because their slot was lost, by business id, until someone reports them. */
    private final Map<String, List<LostNpc>> lostSlots = new HashMap<>();

    /**
     * What the service works with.
     *
     * @param businesses every business
     * @param access     who may create NPCs besides the owner
     * @param gateway    SkysongNPCs
     */
    public record Dependencies(BusinessService businesses, AccessService access, NpcGateway gateway) {
    }

    /**
     * @param warnings told about saved NPCs that could not be kept, as console lines
     */
    public BusinessNpcs(NpcSettings settings, Dependencies dependencies, BusinessNpcRepository repository,
                        BusinessNpcEvents events, Clock clock, Consumer<String> warnings) {
        this.settings = settings;
        this.businesses = dependencies.businesses();
        this.access = dependencies.access();
        this.gateway = dependencies.gateway();
        this.repository = repository;
        this.listeners.add(events);
        this.clock = clock;
        this.warnings = warnings;
    }

    /**
     * Loads every business NPC and brings them in line with their businesses. Rows whose
     * business or NPC is gone are forgotten. Call it after the businesses are loaded.
     *
     * @return how many business NPCs there are
     */
    public int loadAll() {
        byNpc.clear();
        lostSlots.clear();
        List<BusinessNpc> saved = new ArrayList<>(repository.loadAll());
        saved.sort(Comparator.comparing(BusinessNpc::createdAt));
        for (BusinessNpc npc : saved) {
            if (businesses.find(npc.businessId()).isEmpty() || !gateway.exists(npc.npc())) {
                warnings.accept("The NPC " + npc.npc() + " of the business " + npc.businessId()
                        + " no longer exists, so it was forgotten");
                repository.delete(npc.npc());
                continue;
            }
            byNpc.put(npc.npc(), npc);
        }
        for (Business business : businesses.all()) {
            for (BusinessNpc npc : of(business)) {
                if (settings.capacity(business, npc.source()).isEmpty()) {
                    warnings.accept(NpcSettingsParser.FILE + " no longer gives " + business.id() + " NPCs for "
                            + npc.source() + ", so its NPC " + npc.npc() + " is kept as it is");
                }
            }
            sync(business);
        }
        lostSlots.clear();
        return byNpc.size();
    }

    /** Adds something to tell about every NPC a business creates or loses, after the ones added before. */
    public void addListener(BusinessNpcEvents listener) {
        listeners.add(listener);
    }

    private void announceRemoved(Business business, BusinessNpc npc, RemovalCause cause) {
        listeners.forEach(listener -> listener.removed(business, npc, cause));
    }

    /** @return every NPC of the business, oldest first */
    public List<BusinessNpc> of(Business business) {
        return byNpc.values().stream().filter(npc -> npc.businessId().equals(business.id())).toList();
    }

    /** @return the business NPC with that id, empty when the NPC belongs to no business */
    public Optional<BusinessNpc> find(UUID npc) {
        return Optional.ofNullable(byNpc.get(npc));
    }

    /**
     * @return the business's NPC slots by source, in file order: every source that gives it
     *         slots now, then any source that only still has NPCs
     */
    public List<NpcSlotGroup> slots(Business business) {
        Map<SlotSource, List<BusinessNpc>> filled = new LinkedHashMap<>();
        settings.capacities(business).keySet().forEach(source -> filled.put(source, new ArrayList<>()));
        of(business).forEach(npc -> filled.computeIfAbsent(npc.source(), source -> new ArrayList<>()).add(npc));
        List<NpcSlotGroup> groups = new ArrayList<>();
        filled.forEach((source, npcs) -> {
            int capacity = settings.capacity(business, source).orElse(npcs.size());
            if (capacity > 0 || !npcs.isEmpty()) {
                groups.add(new NpcSlotGroup(source, capacity, npcs));
            }
        });
        return groups;
    }

    /** @return whether the business has any NPC slot or NPC, so its NPCs are worth showing */
    public boolean hasSlots(Business business) {
        return !slots(business).isEmpty();
    }

    /**
     * @return whether the character may create NPCs for the business: its owner, or anyone in a
     *         group with the {@code manage-npcs} permission. Staff are the caller's to allow.
     */
    public boolean mayCreate(Business business, CharacterRef character) {
        return access.allows(business, character, Permission.MANAGE_NPCS);
    }

    /**
     * Creates an NPC in one of the source's free slots. Where it appears is set in
     * {@code businesses/npcs.yml}: in front of the creator, or next to the business block. The
     * new NPC is owned by the business's owner and waits for its first setup.
     *
     * @param creator the online player creating it, whose region permissions SkysongNPCs checks
     * @throws BusinessException when the source has no free slot, the business has no block to
     *                           stand next to, or SkysongNPCs could not create the NPC
     */
    public BusinessNpc create(Business business, SlotSource source, UUID creator) {
        NpcSlotGroup group = slots(business).stream().filter(candidate -> candidate.source().equals(source)).findFirst()
                .orElseThrow(() -> new BusinessException("business.npcs.no-such-source", Map.of("source", source.key())));
        String template = settings.template(source).orElse(null);
        if (group.free() == 0 || template == null) {
            throw new BusinessException("business.npcs.no-free-slot");
        }
        NpcCreation creation = gateway.create(template, business.owner().orElse(null), spawnPoint(business, creator),
                settings.spawnRadius());
        if (!creation.isCreated()) {
            throw refusal(creation.outcome(), template);
        }
        BusinessNpc npc = new BusinessNpc(creation.npc().orElseThrow(), business.id(), source, clock.instant());
        byNpc.put(npc.npc(), npc);
        repository.save(npc);
        listeners.forEach(listener -> listener.created(business, npc));
        return npc;
    }

    /**
     * Staff only: removes one of the business's NPCs for good and frees its slot.
     *
     * @return the dismissed NPC, with the name it had
     * @throws BusinessException {@code business.npcs.not-of-business} when the NPC is not this business's
     */
    public LostNpc dismiss(Business business, UUID npc) {
        BusinessNpc found = byNpc.get(npc);
        if (found == null || !found.businessId().equals(business.id())) {
            throw new BusinessException("business.npcs.not-of-business", Map.of("business", business.displayName()));
        }
        return remove(business, found, RemovalCause.DISMISSED);
    }

    /**
     * Brings the business's NPCs in line with it: removes the newest NPCs of every source that
     * has more than its slots, and gives the rest to the business's owner.
     *
     * @return the removed NPCs, newest first
     */
    public List<LostNpc> sync(Business business) {
        List<LostNpc> removed = new ArrayList<>();
        for (NpcSlotGroup group : slots(business)) {
            OptionalInt capacity = settings.capacity(business, group.source());
            if (capacity.isEmpty()) {
                continue;
            }
            List<BusinessNpc> npcs = group.npcs();
            for (int i = npcs.size() - 1; i >= capacity.getAsInt(); i--) {
                removed.add(remove(business, npcs.get(i), RemovalCause.SLOT_LOST));
            }
        }
        @Nullable CharacterRef owner = business.owner().orElse(null);
        for (BusinessNpc npc : of(business)) {
            if (!gateway.isOwnedBy(npc.npc(), owner)) {
                gateway.setOwner(npc.npc(), owner);
            }
        }
        return removed;
    }

    /**
     * @return the NPCs the business lost with their slots since this was last asked, newest
     *         first, so the command that caused it can say so
     */
    public List<LostNpc> takeLostSlots(Business business) {
        List<LostNpc> lost = lostSlots.remove(business.id());
        return lost == null ? List.of() : lost;
    }

    /**
     * An NPC was removed in SkysongNPCs itself. If it was a business NPC its slot is free again.
     */
    public void removedElsewhere(UUID npc) {
        BusinessNpc found = byNpc.get(npc);
        if (found == null) {
            return;
        }
        byNpc.remove(npc);
        repository.delete(npc);
        businesses.find(found.businessId())
                .ifPresent(business -> announceRemoved(business, found, RemovalCause.REMOVED_ELSEWHERE));
    }

    /** A changed business may have fewer slots or another owner. */
    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        List<LostNpc> removed = sync(business);
        if (!removed.isEmpty()) {
            lostSlots.computeIfAbsent(business.id(), id -> new ArrayList<>()).addAll(0, removed);
        }
    }

    /** A deleted business's NPCs are removed; the database drops their rows itself. */
    @Override
    public void deleted(Business business) {
        lostSlots.remove(business.id());
        for (BusinessNpc npc : of(business)) {
            byNpc.remove(npc.npc());
            announceRemoved(business, npc, RemovalCause.BUSINESS_DELETED);
            gateway.remove(npc.npc());
        }
    }

    private LostNpc remove(Business business, BusinessNpc npc, RemovalCause cause) {
        LostNpc lost = new LostNpc(npc, gateway.name(npc.npc()).orElse(npc.npc().toString()));
        // Forgotten first: SkysongNPCs announces the removal, and by then it is no longer ours.
        byNpc.remove(npc.npc());
        repository.delete(npc.npc());
        announceRemoved(business, npc, cause);
        gateway.remove(npc.npc());
        return lost;
    }

    private SpawnPoint spawnPoint(Business business, UUID creator) {
        if (settings.spawnAt() == SpawnAt.PLAYER) {
            return new SpawnPoint.InFrontOf(creator);
        }
        BlockPosition block = business.position()
                .orElseThrow(() -> new BusinessException("business.npcs.no-block", Map.of("business", business.displayName())));
        return new SpawnPoint.NextTo(block, creator);
    }

    private static BusinessException refusal(NpcCreation.Outcome outcome, String template) {
        return switch (outcome) {
            case NO_SPOT -> new BusinessException("business.npcs.no-spot");
            case NOWHERE -> new BusinessException("business.npcs.nowhere");
            case UNKNOWN_TEMPLATE -> new BusinessException("business.npcs.unknown-template", Map.of("template", template));
            case REFUSED, CREATED -> new BusinessException("business.npcs.refused");
        };
    }
}
