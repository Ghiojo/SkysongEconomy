package org.skysongdev.skysongEconomy.gui.node;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** Writes a harvest cost for players, such as "1 downtime, 5 AU", from the {@code node.cost} texts. */
public final class HarvestCostText {
    private HarvestCostText() {
    }

    /** @return every part of the cost joined by {@code node.cost.separator}, or {@code node.cost.free} */
    public static Component describe(Messenger messenger, Wallet wallet, HarvestCost cost) {
        if (cost.isFree()) {
            return messenger.render("node.cost.free");
        }
        List<Component> parts = new ArrayList<>();
        if (cost.downtime() > 0) {
            parts.add(messenger.render("node.cost.downtime", unparsed("amount", String.valueOf(cost.downtime()))));
        }
        if (cost.harvestTokens() > 0) {
            parts.add(messenger.render("node.cost.harvest-tokens", unparsed("amount", String.valueOf(cost.harvestTokens()))));
        }
        if (cost.money().signum() > 0) {
            parts.add(messenger.render("node.cost.money", unparsed("amount", wallet.format(cost.money()))));
        }
        return Component.join(JoinConfiguration.separator(messenger.render("node.cost.separator")), parts);
    }
}
