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

public class AddLedgerEntry implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        float value;
        String materialId = "";
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Usage: /seco ledger add [optional: materialId] (You can also hold the item with an id and just put in the value) <value>"));
            return true;
        }
        if(strings.length < 2){
            if(player.getInventory().getItemInMainHand().getType() == Material.AIR || !player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(SkysongItems.getPlugin(), "material_id")))  {
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>No material ID provided and no item with a material ID held in main hand."));
                return true;
            }
            materialId = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(SkysongItems.getPlugin(), "material_id"), org.bukkit.persistence.PersistentDataType.STRING);
            try {
                value = Float.parseFloat(strings[0]);
            } catch (NumberFormatException e){
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Invalid value provided. Please enter a numeric value."));
                return true;
            }

        } else {
            materialId = strings[0];
            try {
                value = Float.parseFloat(strings[1]);
            } catch (NumberFormatException e){
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Invalid value provided. Please enter a numeric value."));
                return true;
            }
        }

        try {
            if (SkysongEconomy.Instance().getDatabase().findItemCost(materialId) == -1){
                SkysongEconomy.Instance().getDatabase().createLedgerData(materialId, value);
            } else {
                SkysongEconomy.Instance().getDatabase().updateLedgerData(materialId, value);
            }
        } catch (SQLException e) {
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Database error occurred while checking for existing item cost."));
            e.printStackTrace();
            return true;
        }

        commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<green>Ledger entry for material ID " + materialId + " updated with value: " + value));

        return true;
    }
}
