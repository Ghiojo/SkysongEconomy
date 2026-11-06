package org.skysongdev.skysongEconomy.commands.node;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.nodes.NodeFiles;

import java.util.function.Supplier;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se node reload: reads the loot groups, random tables, farm resources and farm types again, so
 * staff can balance without a restart. Broken files are skipped with a console warning and the rest still load.
 * {@code nodes/settings.yml} is only read on start-up.
 */
final class ReloadNodes extends NodeSubCommand {
    private final Supplier<NodeFiles.Loaded> reload;

    /** @param reload reads the files again and puts them in use */
    ReloadNodes(NodeCommandSupport support, Supplier<NodeFiles.Loaded> reload) {
        super(support, "reload", "", 0);
        this.reload = reload;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        NodeFiles.Loaded loaded = reload.get();
        support.messenger.send(sender, "node.reloaded",
                unparsed("groups", String.valueOf(loaded.groups().ids().size())),
                unparsed("tables", String.valueOf(loaded.random().size())),
                unparsed("resources", String.valueOf(loaded.farmResources().size())),
                unparsed("types", String.valueOf(loaded.farmTypes().size())));
        if (loaded.warnings() > 0) {
            support.messenger.send(sender, "node.reload-warnings", unparsed("count", String.valueOf(loaded.warnings())));
        }
    }
}
