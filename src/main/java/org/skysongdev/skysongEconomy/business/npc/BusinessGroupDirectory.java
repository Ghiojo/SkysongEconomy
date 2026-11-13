package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Answers SkysongNPCs' questions about business access groups: who is in
 * {@code economy:<business>/<group>}, and which groups to suggest to someone typing one.
 * <p>
 * It reads the businesses and their groups live, so hiring, firing and group changes count from
 * the next question on. Membership is by character: a player is in a group only while playing
 * the character that is in it. The owner is in no group; SkysongNPCs already lets an NPC's owner
 * do everything. Main thread only.
 */
public final class BusinessGroupDirectory {
    private final BusinessService businesses;
    private final AccessService access;

    public BusinessGroupDirectory(BusinessService businesses, AccessService access) {
        this.businesses = businesses;
        this.access = access;
    }

    /**
     * @param groupId the part after {@code economy:}, such as {@code smithy/workers}
     * @param player  the player's account
     * @param profile the character they are playing, {@code null} when none
     * @return whether that character is in the group; false for a group or business that does not exist
     */
    public boolean contains(String groupId, UUID player, @Nullable String profile) {
        if (profile == null || profile.isBlank()) {
            return false;
        }
        Optional<BusinessGroupRef> ref = BusinessGroupRef.parseId(groupId);
        if (ref.isEmpty()) {
            return false;
        }
        Optional<Business> business = businesses.find(ref.get().businessId());
        if (business.isEmpty()) {
            return false;
        }
        return access.groupsOf(business.get(), new CharacterRef(player, profile)).stream()
                .anyMatch(group -> group.id().equals(ref.get().groupId()));
    }

    /**
     * @return the groups of every business the character owns, as the part after {@code economy:},
     *         in business id order
     */
    public List<String> suggestions(CharacterRef character) {
        List<String> ids = new ArrayList<>();
        businesses.all().stream()
                .filter(business -> business.isOwner(character))
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .forEach(business -> {
                    for (AccessGroup group : access.groups(business)) {
                        ids.add(new BusinessGroupRef(business.id(), group.id()).id());
                    }
                });
        return ids;
    }
}
