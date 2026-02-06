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
        Player player = (Player) commandSender;
        if(strings.length < 1 && (player.getInventory().getItemInMainHand().getType() == org.bukkit.Material.AIR || !player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(SkysongItems.getPlugin(), "material_id")))){
            commandSender.sendMessage("Usage: /seco ledger remove <entryID> (or hold item with a material id)");
            return true;
        }
        String materialId;

        if(strings.length >= 1){
            materialId = strings[0];
        } else {
            materialId = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(SkysongItems.getPlugin(), "material_id"), org.bukkit.persistence.PersistentDataType.STRING);
        }

        try {
            SkysongEconomy.Instance().getDatabase().deleteLedgerData(materialId);
        } catch (SQLException e) {
            e.printStackTrace();
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Failed to remove ledger entry due to a database error."));
            return true;
        }

        return true;
    }
}
