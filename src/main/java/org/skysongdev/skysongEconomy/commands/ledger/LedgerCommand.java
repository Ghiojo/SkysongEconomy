package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.SkysongEconomy;

import java.util.List;

import static org.skysongdev.skysongEconomy.utils.Constants.PLUGIN_TAG;

public class LedgerCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length < 1){
            commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "Usage: /skysongeconomy ledger <subcommand>"));
            return true;
        }
        String[] newArgs = java.util.Arrays.copyOfRange(strings, 1, strings.length);
        switch(strings[0].toLowerCase()){
            case "view":
                ViewLedger viewLedger = new ViewLedger();
                viewLedger.onCommand(commandSender, command, s, newArgs);
                break;
            case "add":
                if(commandSender.hasPermission("skysongeconomy.ledger.add")){
                    AddLedgerEntry addLedgerEntry = new AddLedgerEntry();
                    addLedgerEntry.onCommand(commandSender, command, s, newArgs);
                } else {
                    commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "You do not have permission to use this command."));
                }
                break;
            case "remove":
                if(commandSender.hasPermission("skysongeconomy.ledger.remove")){
                    RemoveLedgerEntry removeLedgerEntry = new RemoveLedgerEntry();
                    removeLedgerEntry.onCommand(commandSender, command, s, newArgs);
                } else {
                    commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "You do not have permission to use this command."));
                }
                break;
            default:
                commandSender.sendMessage(SkysongEconomy.Instance().getMm().deserialize(PLUGIN_TAG + "Unknown subcommand."));
                break;
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length != 1){
            return List.of();
        }
        return List.of("view", "add", "remove").stream().filter(a -> a.startsWith(strings[0].toLowerCase())).toList();
    }
}
