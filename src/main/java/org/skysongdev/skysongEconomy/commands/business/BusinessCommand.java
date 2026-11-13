package org.skysongdev.skysongEconomy.commands.business;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleWorkerSlots;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargets;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.access.BusinessNpcAccess;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;

/**
 * /se business: creating and managing businesses. Staff commands need
 * {@code skysongeconomy.business.admin}; owner commands check ownership themselves.
 */
import java.util.function.Function;
import java.util.Optional;

public final class BusinessCommand extends CommandGroup {
    /** Set once modules are enabled. */
    private @Nullable ModuleCommandText moduleText;
    private final BusinessCommandSupport support;

    public BusinessCommand(BusinessService businesses, BusinessTypeRegistry types, AccessService access, BusinessVaults vaults,
                           CharacterDirectory characters, Messenger messenger) {
        super("business", "skysongeconomy.business", messenger);
        this.support = new BusinessCommandSupport(businesses, characters, messenger);
        register(new CreateBusiness(support, types));
        register(new DeleteBusiness(support));
        register(new SetBusinessTier(support));
        register(new SetBusinessOwner(support));
        register(new MoveBusiness(support));
        register(new RenameBusiness(support));
        register(new SetShiftMessage(support));
        register(new HireWorker(support));
        register(new FireWorker(support));
        register(new ClearSkillPick(support));
        register(new AccessGroupCommand(support, access));
        register(new VaultCommand(support, vaults));
        register(new BusinessInfo(support, types));
        register(new ListBusinesses(support));
    }

    /**
     * Adds what business NPCs bring: {@code npcs}, {@code dismissnpc} and {@code npcsync}, and
     * telling staff which NPCs were removed when {@code settier} or {@code clearpick} took their slots. Call it once,
     * when SkysongNPCs is installed.
     *
     * @param npcs    the NPCs each business has
     * @param gateway SkysongNPCs, for names
     * @param sheets  what each NPC still has to choose
     * @param skills  skill names
     * @param progression what each NPC is owed, for {@code npcsync}
     * @param moduleNames a module's name by its id, for the slots modules give
     */
    public void enableNpcs(BusinessNpcs npcs, NpcGateway gateway, NpcSheetGateway sheets, SkillCatalog skills,
                           NpcProgression progression, Function<String, Optional<String>> moduleNames) {
        NpcCommandText text = new NpcCommandText(npcs, gateway, sheets, skills, moduleNames, support.messenger);
        register(new ListBusinessNpcs(support, text));
        register(new DismissNpc(support, text));
        register(new SyncBusinessNpcs(support, text, progression));
        support.addChangeReport(text);
    }

    /**
     * Adds what business modules bring: {@code modules} and {@code clearmodule}, the modules in
     * {@code info}, and telling staff which modules were dropped when {@code settier} or
     * {@code clearpick} took their picks. Call it once.
     *
     * @param modules the modules each business picked
     * @param workers the workers a business loses with a module
     * @param skills  skill names
     */
    public void enableModules(BusinessModules modules, ModuleWorkerSlots workers, SkillCatalog skills) {
        ModuleCommandText text = new ModuleCommandText(support, modules, workers, skills);
        moduleText = text;
        register(new ListBusinessModules(support, text));
        register(new ClearModule(support, text));
        support.addChangeReport(text);
        support.addInfoReport((sender, business) -> {
            if (modules.hasPicks(business)) {
                support.messenger.send(sender, "business.modules.info-header");
                text.list(sender, business);
            }
        });
    }

    /** Lets {@code modules} and {@code info} say how many NPCs are still to choose for each module. */
    public void enableModuleNpcs(ModuleTargets targets) {
        if (moduleText != null) {
            moduleText.enableNpcs(targets);
        }
    }

    /**
     * Adds {@code npcaccess}: what each access group may do with a business's NPCs. Call it
     * once, when SkysongNPCs is installed.
     */
    public void enableNpcAccess(AccessService access, BusinessNpcAccess npcAccess) {
        register(new NpcAccessCommand(support, access, npcAccess));
    }
}
