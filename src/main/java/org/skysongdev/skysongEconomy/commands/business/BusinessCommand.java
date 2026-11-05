package org.skysongdev.skysongEconomy.commands.business;

import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.messaging.Messenger;

/**
 * /se business: creating and managing businesses. Staff commands need
 * {@code skysongeconomy.business.admin}; owner commands check ownership themselves.
 */
public final class BusinessCommand extends CommandGroup {
    public BusinessCommand(BusinessService businesses, BusinessTypeRegistry types, AccessService access, BusinessVaults vaults,
                           CharacterDirectory characters, Messenger messenger) {
        super("business", "skysongeconomy.business", messenger);
        BusinessCommandSupport support = new BusinessCommandSupport(businesses, characters, messenger);
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
}
