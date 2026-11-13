package org.skysongdev.skysongEconomy.business.npc;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who may do what with NPCs, through SkysongNPCs' access policies. Main thread only.
 * <p>
 * A policy is a named list of grants; each grant lets one group do some actions with every NPC
 * that follows the policy. Economy keeps one policy per business, so changing a group's actions
 * changes all of that business's NPCs at once. SkysongNPCs stores the policies; nothing here is
 * saved by Economy.
 * <p>
 * Actions are SkysongNPCs' action names, such as {@code inventory-view}. Groups are written as
 * SkysongNPCs writes them, {@code <type>:<id>}; a business group is
 * {@link BusinessGroupRef#toString()}.
 */
public interface NpcAccessGateway {
    /** The longest policy id SkysongNPCs accepts; ids are lower case letters, digits, {@code _ . -}. */
    int MAX_POLICY_ID_LENGTH = 64;

    /** @return every action name, in SkysongNPCs' order */
    Set<String> actions();

    /** @return the actions a SkysongNPCs preset such as {@code customer} stands for; empty when there is no such preset */
    Optional<Set<String>> preset(String name);

    /** @return whether a policy with that id exists */
    boolean hasPolicy(String policyId);

    /**
     * Makes an empty policy owned by this plugin, unless one with that id exists already.
     *
     * @return true when it was made now
     */
    boolean createPolicy(String policyId);

    /**
     * Deletes a policy. Its NPCs stop following it.
     *
     * @return false when there was none
     */
    boolean deletePolicy(String policyId);

    /** @return what each group may do under the policy, by group; empty when there is no such policy */
    Map<String, Set<String>> grants(String policyId);

    /**
     * Sets exactly what one group may do under the policy.
     *
     * @param group   the group, as {@code <type>:<id>}
     * @param actions the action names it may do from now on; empty lets it do nothing
     * @throws IllegalArgumentException when there is no such policy, or an action name is unknown
     */
    void setGrant(String policyId, String group, Set<String> actions);

    /** Makes an NPC follow a policy, instead of the one it followed before. */
    void assign(UUID npc, String policyId);

    /**
     * Makes an NPC follow no policy.
     *
     * @return false when it followed none
     */
    boolean unassign(UUID npc);

    /** @return the id of the policy the NPC follows, empty when none */
    Optional<String> policyOf(UUID npc);
}
