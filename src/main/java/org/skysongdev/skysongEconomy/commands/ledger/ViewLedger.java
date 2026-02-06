package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.Material;
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

public class ViewLedger implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(player.getInventory().getItemInMainHand().getType() == Material.AIR || !player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(SkysongItems.getPlugin(), "material_id"))){
            player.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>You must be holding a valid item to view its ledger."));
            return true;
        }

        String materialID = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(SkysongItems.getPlugin(), "material_id"), org.bukkit.persistence.PersistentDataType.STRING);
        float value = -1;
        try {
            value = SkysongEconomy.Instance().getDatabase().findItemCost(materialID);
        } catch (SQLException e){
            e.printStackTrace();
            player.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>An error occurred while trying to retrieve the item's cost."));
            return true;
        }

        if(value == -1){
            player.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>This item does not have a cost set in the ledger."));
            return true;
        }

        player.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<green>The current cost of this item is: <gold>" + value));

        return true;
    }
}
