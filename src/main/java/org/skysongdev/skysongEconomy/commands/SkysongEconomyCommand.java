package org.skysongdev.skysongEconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.commands.good.GoodCommand;
import org.skysongdev.skysongEconomy.commands.ledger.LedgerCommand;

import java.util.List;

import static org.skysongdev.skysongEconomy.utils.Constants.PLUGIN_TAG;

public class SkysongEconomyCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length < 1){
            commandSender.sendMessage(PLUGIN_TAG + "Usage: /skysongeconomy <subcommand> [args]");
            return true;
        }
        String[] newArgs = java.util.Arrays.copyOfRange(strings, 1, strings.length);
        switch(strings[0].toLowerCase()){
            case "ledger":
                if(commandSender.hasPermission("skysongeconomy.ledger")){
                    LedgerCommand ledgerCommand = new LedgerCommand();
                    ledgerCommand.onCommand(commandSender, command, s, newArgs);
                } else {
                    commandSender.sendMessage(PLUGIN_TAG + "You do not have permission to use this command.");
                }
                break;
            case "good":
                if(commandSender.hasPermission("skysongeconomy.good")){
                    GoodCommand goodCommand = new GoodCommand();
                    goodCommand.onCommand(commandSender, command, s, newArgs);
                } else {
                    commandSender.sendMessage(PLUGIN_TAG + "You do not have permission to use this command.");
                }
                break;
            default:
                commandSender.sendMessage(PLUGIN_TAG + "Unknown subcommand.");
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length != 1){
            return List.of();
        }
        return List.of("ledger", "good").stream().filter(a -> a.startsWith(strings[0].toLowerCase())).toList();
    }
}
