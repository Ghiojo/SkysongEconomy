package org.skysongdev.skysongEconomy.gui.business;

import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Optional;

/**
 * Who is looking at which business menu.
 *
 * @param character the character the viewer is playing, empty when they have none
 * @param staff     whether the viewer has the business staff permission
 */
public record BusinessMenuContext(Player viewer, Optional<CharacterRef> character, Business business, boolean staff) {
    /** @return whether the viewer's character owns the business */
    public boolean isOwner() {
        return character.filter(business::isOwner).isPresent();
    }

    /**
     * @return whether the viewer may do what only an owner does (choose skills, select ledgers):
     *         the owner, or staff when the server owns the business
     */
    public boolean actsAsOwner() {
        return isOwner() || (staff && business.isServerOwned());
    }

    /** @return whether the viewer's character is the owner or a worker */
    public boolean isMember() {
        return character.filter(business::isMember).isPresent();
    }

    /** @return whether the viewer may manage the business: its owner, or staff */
    public boolean canManage() {
        return staff || isOwner();
    }
}
