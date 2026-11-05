package org.skysongdev.skysongEconomy.commands.good;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.SkysongEconomy;
import org.skysongdev.skysongitems.SkysongItems;

import java.sql.SQLException;

import static org.skysongdev.skysongEconomy.utils.Constants.PLUGIN_TAG;

public class RemoveGoodAssociation implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String goodType;
        String materialId;
        if(strings.length < 1){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Usage: /seco good remove [materialId] <goodType>"));
            return true;
        }
        if(strings.length == 1){
            if(!(commandSender instanceof Player player)){
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>From the console you must specify a material ID: /seco good remove <materialId> <goodType>"));
                return true;
            }
            if(player.getInventory().getItemInMainHand().getType() == Material.AIR || !player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(SkysongItems.getPlugin(), "material_id"))){
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>You must either specify a material ID or hold an item with a material ID in your main hand."));
                return true;
            } else {
                materialId = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(SkysongItems.getPlugin(), "material_id"), PersistentDataType.STRING);
                goodType = strings[0];
            }
        } else {
            materialId = strings[0];
            goodType = strings[1];
        }

        try{
            SkysongEconomy.Instance().getDatabase().deleteGoodTypeAssociation(goodType, materialId);
        } catch(SQLException e){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>There was an error removing the good association: " + e.getMessage()));
            return true;
        }

        commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<green>Successfully removed association of good type <yellow>" + goodType + "<green> from material ID <yellow>" + materialId + "<green>."));
        return true;
    }
}
