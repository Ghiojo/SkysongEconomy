package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.character.CharacterDirectory;

import java.util.UUID;

/**
 * Who may open a bench because of the business it belongs to: the business's owner and workers,
 * while they play that character. Asked only when SkysongCrafting would refuse them, so it only
 * ever adds access. Checked on every click, so it follows hires, fires and owner changes at once.
 */
public final class BenchAccess {
    private final BenchSettings settings;
    private final BenchRegistry benches;
    private final BusinessService businesses;
    private final CharacterDirectory characters;

    public BenchAccess(BenchSettings settings, BenchRegistry benches, BusinessService businesses,
                       CharacterDirectory characters) {
        this.settings = settings;
        this.benches = benches;
        this.businesses = businesses;
        this.characters = characters;
    }

    /** @return whether the player's active character owns or works at the business the bench belongs to */
    public boolean mayUse(BlockPosition bench, UUID player) {
        if (!settings.businessStaffCanUse()) {
            return false;
        }
        return benches.at(bench)
                .flatMap(link -> businesses.find(link.businessId()))
                .flatMap(business -> characters.active(player).map(business::isMember))
                .orElse(false);
    }
}
