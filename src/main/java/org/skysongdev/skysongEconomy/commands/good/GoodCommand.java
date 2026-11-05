package org.skysongdev.skysongEconomy.commands.good;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GoodCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length < 1){
            commandSender.sendMessage("Usage: /skysongeconomy good <subcommand> [args]");
            return true;
        }
        String[] newArgs = java.util.Arrays.copyOfRange(strings, 1, strings.length);
        switch(strings[0].toLowerCase()){
            // Add subcommands here
            case "add":
                if(commandSender.hasPermission("skysongeconomy.good.manage")){
                    AddGoodAssociation addGood = new AddGoodAssociation();
                    addGood.onCommand(commandSender, command, s, newArgs);
                    break;
                } else {
                    commandSender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

            case "remove":
                if(commandSender.hasPermission("skysongeconomy.good.manage")){
                    RemoveGoodAssociation removeGood = new RemoveGoodAssociation();
                    removeGood.onCommand(commandSender, command, s, newArgs);
                    break;
                } else {
                    commandSender.sendMessage("You do not have permission to use this command.");
                    return true;
                }
            default:
                commandSender.sendMessage("Unknown subcommand.");
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length != 1){
            return List.of();
        }
        return List.of("add", "remove").stream().filter(a -> a.startsWith(strings[0].toLowerCase())).toList();
    }
}
