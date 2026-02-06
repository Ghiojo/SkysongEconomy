package org.skysongdev.skysongEconomy.commands.good;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.SkysongEconomy;
import org.skysongdev.skysongitems.SkysongItems;

import java.sql.SQLException;

import static org.skysongdev.skysongEconomy.utils.Constants.PLUGIN_TAG;

public class AddGoodAssociation implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String goodType;
        String materialId;
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Usage: /seco good add <materialId> <goodType>"));
            return true;
        }
        if(strings.length == 1){
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

        try {
            SkysongEconomy.Instance().getDatabase().createGoodTypeAssociation(goodType, materialId);
        } catch (SQLException e){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<red>Failed to associate material ID <yellow>" + materialId + " <red> with good type <yellow>" + goodType + "<red>. Error: " + e.getMessage()));
            return true;
        }
        commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "<green>Successfully associated material ID <yellow>" + materialId + " <green> with good type <yellow>" + goodType + "<green>."));

        return true;
    }
}
