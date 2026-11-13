package org.skysongdev.skysongEconomy.business.module.npc;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Which NPCs an owner chose for the module benefits that go to a number of NPCs.
 * <p>
 * A choice is permanent for that NPC. Only when the NPC is removed does its share open again,
 * for the owner to choose who gets it next; when the module pick is removed its choices go with
 * it. Not thread-safe: use it on the main thread.
 */
public final class ModuleTargets {
    private final BusinessModules modules;
    private final BusinessNpcs npcs;
    private final ModuleTargetRepository repository;
    /** The chosen NPCs of each effect of each pick, in the order they were chosen. */
    private final Map<Key, List<UUID>> chosen = new LinkedHashMap<>();

    private record Key(String businessId, int pickOrder, int effect) {
    }

    public ModuleTargets(BusinessModules modules, BusinessNpcs npcs, ModuleTargetRepository repository) {
        this.modules = modules;
        this.npcs = npcs;
        this.repository = repository;
    }

    /**
     * Loads every choice. Choices of an NPC that is no longer a business NPC are forgotten. Call
     * it after the module picks and the business NPCs are loaded.
     *
     * @return how many choices there are
     */
    public int loadAll() {
        chosen.clear();
        int count = 0;
        for (ModuleTargetRepository.Row row : repository.loadAll()) {
            if (npcs.find(row.npc()).isEmpty()) {
                repository.deleteNpc(row.npc());
                continue;
            }
            chosen.computeIfAbsent(new Key(row.businessId(), row.pickOrder(), row.effect()), key -> new ArrayList<>())
                    .add(row.npc());
            count++;
        }
        return count;
    }

    /** @return every benefit of the business's modules that goes to a number of NPCs, oldest pick first */
    public List<ModuleShare> shares(Business business) {
        List<ModuleShare> shares = new ArrayList<>();
        for (ModulePick pick : modules.of(business)) {
            Optional<Module> module = modules.module(pick.moduleId());
            if (module.isEmpty()) {
                continue;
            }
            List<ModuleEffect> effects = module.get().effects();
            for (int i = 0; i < effects.size(); i++) {
                if (effects.get(i) instanceof ModuleEffect.NpcBenefit benefit && !benefit.target().isAll()) {
                    shares.add(new ModuleShare(pick, module.get(), i, benefit, chosen(pick, i)));
                }
            }
        }
        return shares;
    }

    /** @return the shares of one module pick */
    public List<ModuleShare> shares(Business business, ModulePick pick) {
        return shares(business).stream().filter(share -> share.pick().equals(pick)).toList();
    }

    /** @return how many NPCs the owner can still choose, over all the business's modules */
    public int open(Business business) {
        return shares(business).stream().mapToInt(ModuleShare::open).sum();
    }

    /** @return the NPCs that can be chosen for the share: the ones that count and are not chosen yet, oldest first */
    public List<BusinessNpc> eligible(Business business, ModuleShare share) {
        return npcs.of(business).stream()
                .filter(npc -> share.benefit().target().counts(npc) && !share.chosen().contains(npc.npc())).toList();
    }

    /** @return whether the NPC was chosen for that effect of that pick */
    public boolean isChosen(ModulePick pick, int effect, UUID npc) {
        return chosen(pick, effect).contains(npc);
    }

    /**
     * Chooses an NPC for a share. The choice is permanent for that NPC.
     *
     * @throws BusinessException when the share is full, or the NPC does not count or is chosen already
     */
    public void choose(Business business, ModuleShare share, UUID npc) {
        ModuleShare now = shares(business, share.pick()).stream().filter(candidate -> candidate.effect() == share.effect())
                .findFirst().orElseThrow(() -> new BusinessException("business.modules.assign.gone"));
        if (now.open() == 0) {
            throw new BusinessException("business.modules.assign.full");
        }
        if (eligible(business, now).stream().noneMatch(candidate -> candidate.npc().equals(npc))) {
            throw new BusinessException("business.modules.assign.not-eligible");
        }
        ModulePick pick = now.pick();
        chosen.computeIfAbsent(new Key(pick.businessId(), pick.order(), now.effect()), key -> new ArrayList<>()).add(npc);
        repository.save(new ModuleTargetRepository.Row(pick.businessId(), pick.order(), now.effect(), npc));
    }

    /** An NPC is gone: its shares open again. */
    public void npcRemoved(UUID npc) {
        boolean[] any = {false};
        chosen.values().forEach(npcsOfShare -> any[0] |= npcsOfShare.remove(npc));
        if (any[0]) {
            chosen.values().removeIf(List::isEmpty);
            repository.deleteNpc(npc);
        }
    }

    /** A module pick is gone: its choices go with it. */
    public void pickRemoved(ModulePick pick) {
        if (chosen.keySet().removeIf(key -> key.businessId().equals(pick.businessId()) && key.pickOrder() == pick.order())) {
            repository.deletePick(pick.businessId(), pick.order());
        }
    }

    /** A business is gone: its choices go with it; the database drops their rows itself. */
    public void businessDeleted(Business business) {
        chosen.keySet().removeIf(key -> key.businessId().equals(business.id()));
    }

    private List<UUID> chosen(ModulePick pick, int effect) {
        return chosen.getOrDefault(new Key(pick.businessId(), pick.order(), effect), List.of());
    }
}
