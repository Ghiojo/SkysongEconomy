package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.gui.Menu;
import org.skysongdev.skysongEconomy.gui.MenuButton;
import org.skysongdev.skysongEconomy.items.ItemStackBytes;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.farm.FarmHarvester;
import org.skysongdev.skysongEconomy.nodes.farm.FarmPlots;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.farm.FarmTier;
import org.skysongdev.skysongEconomy.nodes.farm.FarmType;
import org.skysongdev.skysongEconomy.nodes.farm.Planting;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * The menu of a farm node: its details on top, one button per tier of what is planted, and the
 * planting slot below. Tenders put a primer in the slot to plant it, for free: once the slot
 * holds the whole primer (both of a pair, say) it is planted, and anything not planted goes back
 * when the menu closes. The button beside the slot takes the primer out. Each click on a tier is
 * one instant harvest, and the menu stays open.
 */
final class FarmMenu extends Menu {
    private static final int SIZE = 27;
    private static final int INFO = 4;
    /** Tier N goes in slot FIRST_TIER + N - 1, as in the random node menu. */
    private static final int FIRST_TIER = 11;
    private static final int PLOT = 20;
    private static final int CLOSE = 22;
    private static final int TAKE_OUT = 24;

    private final NodeMenus menus;
    private final String nodeId;
    private final CharacterRef character;

    FarmMenu(NodeMenus menus, String nodeId, CharacterRef character) {
        super(SIZE, menus.messenger().render("node.gather.title",
                parsed("node", menus.deps().nodes().find(nodeId).map(menus.deps().nodes()::name).orElse(nodeId))));
        this.menus = menus;
        this.nodeId = nodeId;
        this.character = character;
    }

    @Override
    protected void draw() {
        MenuButton border = menus.theme().border();
        for (int slot = 0; slot < SIZE; slot++) {
            set(slot, border);
        }
        set(CLOSE, menus.theme().close());
        Optional<Node> found = menus.deps().nodes().find(nodeId);
        if (found.isEmpty()) {
            return;
        }
        Node node = found.get();
        FarmPlots plots = menus.deps().farms().plots();
        FarmType type;
        try {
            type = plots.type(node);
        } catch (RefusedException unavailable) {
            return;
        }
        Optional<Planting> planting = plots.planting(node);
        boolean tender = plots.canTend(node, character);
        set(INFO, MenuButton.display(info(node, planting, tender)));
        set(PLOT, MenuButton.display(plot(planting, tender)));
        if (planting.isPresent() && tender) {
            set(TAKE_OUT, takeOutButton());
        }
        FarmHarvester harvester = menus.deps().farms().harvester();
        for (FarmTier tier : harvester.offered(node)) {
            set(FIRST_TIER + tier.tier() - 1, menus.text().tierButton(character, tier.tier(),
                    Optional.of(HarvestCost.tokens(tier.cost())), harvester.requiredLevel(tier), type.skill(),
                    tier.output(), click -> {
                        menus.harvest(click.player(), nodeId, tier.tier());
                        redraw();
                    }));
        }
    }

    /** The planting slot takes primers while nothing is planted. */
    @Override
    protected boolean isInputSlot(int slot) {
        return slot == PLOT && menus.deps().nodes().find(nodeId)
                .filter(node -> menus.deps().farms().plots().planting(node).isEmpty()).isPresent();
    }

    /** Takes what the primer still lacks, of one kind of item only. */
    @Override
    protected int accepts(Player player, int slot, ItemStack offered, List<ItemStack> held) {
        if (!held.isEmpty() && !held.getFirst().isSimilar(offered)) {
            menus.messenger().send(player, "node.farm.one-primer");
            return 0;
        }
        Optional<FarmResource> resource = menus.primerFor(player, nodeId, offered);
        int needed = resource.map(found -> found.primer().amount() - amount(held)).orElse(0);
        return Math.max(0, Math.min(needed, offered.getAmount()));
    }

    /** Plants as soon as the slot holds the whole primer, such as both of a pair. */
    @Override
    protected void received(Player player, int slot) {
        List<ItemStack> held = held(slot);
        Optional<FarmResource> resource = menus.primerFor(player, nodeId, held.getFirst());
        if (resource.isEmpty()) {
            return;
        }
        int needed = resource.get().primer().amount();
        if (amount(held) < needed) {
            menus.messenger().send(player, "node.farm.needs-more", unparsed("amount", String.valueOf(needed - amount(held))),
                    parsed("resource", resource.get().name()));
        } else if (menus.plant(player, nodeId, resource.get(), held)) {
            useHeld(slot);
        }
    }

    private static int amount(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getAmount).sum();
    }

    private ItemStack info(Node node, Optional<Planting> planting, boolean tender) {
        List<Component> lore = new ArrayList<>();
        lore.add(line("node.gather.info.owner", component("owner", menus.text().owner(node))));
        lore.add(line("node.farm.info.planted", component("resource", plantedName(planting))));
        lore.add(line("node.gather.info.tokens",
                unparsed("tokens", String.valueOf(menus.deps().tokens().balance(character)))));
        lore.add(line(planting.isEmpty() ? (tender ? "node.farm.info.hint-plant" : "node.farm.info.hint-empty")
                : "node.gather.info.hint"));
        return menus.theme().item(menus.icons().info(), line("node.gather.info.name",
                parsed("node", menus.deps().nodes().name(node)), unparsed("tier", String.valueOf(node.tier()))), lore);
    }

    private ItemStack plot(Optional<Planting> planting, boolean tender) {
        if (planting.isEmpty()) {
            return menus.theme().item(menus.icons().emptyPlot(), line("node.farm.plot.empty-name"),
                    List.of(line(tender ? "node.farm.plot.empty-tender" : "node.farm.plot.empty")));
        }
        Material icon = ItemStackBytes.restore(planting.get().primer()).getFirst().getType();
        return menus.theme().item(icon, line("node.farm.plot.planted-name", component("resource", plantedName(planting))),
                List.of(line("node.farm.plot.planted")));
    }

    private MenuButton takeOutButton() {
        int cost = menus.deps().farms().plots().removalDowntime();
        TagResolver downtime = unparsed("downtime", String.valueOf(cost));
        return MenuButton.of(menus.theme().item(menus.icons().takeOut(), line("node.farm.take-out.name"),
                        List.of(line(cost > 0 ? "node.farm.take-out.cost" : "node.farm.take-out.free", downtime),
                                line("node.farm.take-out.click"))),
                click -> {
                    menus.takeOut(click.player(), nodeId);
                    redraw();
                });
    }

    /** @return the planted resource's name, or "Empty" */
    private Component plantedName(Optional<Planting> planting) {
        return menus.deps().nodes().find(nodeId).flatMap(menus.deps().farms().plots()::plantedName)
                .map(name -> menus.messenger().renderText(name))
                .orElseGet(() -> line("node.farm.empty-name"));
    }

    private Component line(String key, TagResolver... placeholders) {
        return menus.text().line(key, placeholders);
    }
}
