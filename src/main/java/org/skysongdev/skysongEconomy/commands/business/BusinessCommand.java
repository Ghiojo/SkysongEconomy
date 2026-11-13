package org.skysongdev.skysongEconomy.commands.business;

import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcGateway;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
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
public final class BusinessCommand extends CommandGroup {
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
     * Adds what business NPCs bring: {@code npcs} and {@code dismissnpc}, and telling staff which
     * NPCs were removed when {@code settier} or {@code clearpick} took their slots. Call it once,
     * when SkysongNPCs is installed.
     *
     * @param npcs    the NPCs each business has
     * @param gateway SkysongNPCs, for names
     * @param sheets  what each NPC still has to choose
     * @param skills  skill names
     */
    public void enableNpcs(BusinessNpcs npcs, NpcGateway gateway, NpcSheetGateway sheets, SkillCatalog skills) {
        NpcCommandText text = new NpcCommandText(npcs, gateway, sheets, skills, support.messenger);
        register(new ListBusinessNpcs(support, text));
        register(new DismissNpc(support, text));
        support.addChangeReport(text);
    }
}
