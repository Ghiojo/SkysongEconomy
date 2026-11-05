package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.SkysongEconomy;
import org.skysongdev.skysongitems.SkysongItems;

import java.sql.SQLException;

import static org.skysongdev.skysongEconomy.utils.Constants.PLUGIN_TAG;

public class RemoveLedgerEntry implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String materialId;

        if(strings.length >= 1){
            materialId = strings[0];
        } else if(commandSender instanceof Player player
                && player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR
                && player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(SkysongItems.getPlugin(), "material_id"))){
            materialId = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(SkysongItems.getPlugin(), "material_id"), org.bukkit.persistence.PersistentDataType.STRING);
        } else {
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Usage: /seco ledger remove <materialId> (or hold an item with a material ID)"));
            return true;
        }

        try {
            SkysongEconomy.Instance().getDatabase().deleteLedgerData(materialId);
        } catch (SQLException e) {
            e.printStackTrace();
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Failed to remove ledger entry due to a database error."));
            return true;
        }

        commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<green>Removed the ledger entry for material ID <yellow>" + materialId + "<green>."));
        return true;
    }
}
