package org.skysongdev.skysongEconomy.gui.node;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.Menu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

/**
 * What every node menu shares: the border, the close button, the tier buttons' slots and the
 * booster slot. A player puts one booster in the booster slot; each harvest they click while it
 * is there is boosted and uses it once, and closing the menu gives back what is left of it.
 */
abstract class HarvestMenu extends Menu {
    protected static final int SIZE = 27;
    protected static final int INFO = 4;
    /** Tier N goes in slot FIRST_TIER + N - 1, so tiers 1 to 5 fill the middle of the second row. */
    protected static final int FIRST_TIER = 11;
    protected static final int BOOSTER = 18;
    protected static final int CLOSE = 22;

    protected final NodeMenus menus;
    protected final String nodeId;
    protected final CharacterRef character;

    HarvestMenu(NodeMenus menus, String nodeId, CharacterRef character) {
        super(SIZE, menus.messenger().render("node.gather.title",
                parsed("node", menus.deps().nodes().find(nodeId).map(menus.deps().nodes()::name).orElse(nodeId))));
        this.menus = menus;
        this.nodeId = nodeId;
        this.character = character;
    }

    @Override
    protected final void draw() {
        MenuButton border = menus.theme().border();
        for (int slot = 0; slot < SIZE; slot++) {
            set(slot, border);
        }
        set(CLOSE, menus.theme().close());
        set(BOOSTER, MenuButton.display(menus.theme().item(menus.icons().booster(),
                menus.text().line("node.booster.slot-name"), List.of(menus.text().line("node.booster.slot-empty"),
                        menus.text().line("node.booster.slot-take-back")))));
        menus.deps().nodes().find(nodeId).ifPresent(this::drawNode);
    }

    /** Puts the node's own buttons in place, over the border. */
    protected abstract void drawNode(Node node);

    /** @return a tier's button, which harvests once per click with the held booster, if any */
    protected final MenuButton tierButton(int tier, Optional<HarvestCost> cost, String level, String skill,
                                          LootOutput output) {
        return menus.text().tierButton(character, tier, cost, level, skill, output, click -> {
            harvest(click.player(), tier);
            // The token balance, the player's level and the booster may have changed.
            redraw();
        });
    }

    private void harvest(Player player, int tier) {
        Optional<ItemStack> booster = held(BOOSTER).stream().findFirst();
        if (menus.harvest(player, nodeId, tier, booster)) {
            booster.ifPresent(used -> setHeld(BOOSTER, menus.useBooster(player, used).stream().toList()));
        }
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == BOOSTER;
    }

    /**
     * A shift-clicked booster only goes to the booster slot, and anything else only to the other
     * input slots, so the player is only told why the slot meant for it refused it.
     */
    @Override
    protected List<Integer> shiftClickSlots(ItemStack stack) {
        if (menus.isBooster(stack)) {
            return List.of(BOOSTER);
        }
        List<Integer> others = inputSlots().stream().filter(slot -> slot != BOOSTER).toList();
        return others.isEmpty() ? List.of(BOOSTER) : others;
    }

    /** The booster slot takes one booster that works on this node. */
    @Override
    protected int accepts(Player player, int slot, ItemStack offered, List<ItemStack> held) {
        if (slot != BOOSTER) {
            return 0;
        }
        if (!held.isEmpty()) {
            menus.messenger().send(player, "node.booster.slot-full");
            return 0;
        }
        return menus.boosterFor(player, nodeId, offered).isPresent() ? 1 : 0;
    }
}
