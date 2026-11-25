package org.skysongdev.skysongEconomy.commands.journal;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.journal.EconomyJournal;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.journal.HistoryLine;
import org.skysongdev.skysongEconomy.journal.HistoryRequest;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.Wallet;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /se history [business <id> | pier <id> | player <name> | kind <kind>] [page]}: the economy
 * journal, ten records a page, newest first.
 */
public final class HistoryCommand implements SubCommand {
    /** Who may read the economy history. */
    public static final String PERMISSION = "skysongeconomy.history";

    private final EconomyJournal journal;
    private final Messenger messenger;
    private final Wallet wallet;
    private final Logger logger;

    public HistoryCommand(EconomyJournal journal, Messenger messenger, Wallet wallet, Logger logger) {
        this.journal = journal;
        this.messenger = messenger;
        this.wallet = wallet;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "history";
    }

    @Override
    public String permission() {
        return PERMISSION;
    }

    @Override
    public String usage() {
        return HistoryRequest.usage();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        HistoryRequest request;
        try {
            request = HistoryRequest.parse(args, HistoryCommand::playerNamed);
        } catch (RefusedException refused) {
            messenger.send(sender, refused);
            return;
        }
        journal.query(request.query()).whenComplete((records, failure) -> {
            if (failure != null) {
                logger.log(Level.SEVERE, "/se history could not read the economy journal", failure);
                messenger.send(sender, "history.failed");
                return;
            }
            if (records.isEmpty()) {
                messenger.send(sender, "history.empty", Placeholder.unparsed("filter", request.filter()),
                        Placeholder.unparsed("page", String.valueOf(request.page())));
                return;
            }
            messenger.send(sender, "history.header", Placeholder.unparsed("filter", request.filter()),
                    Placeholder.unparsed("page", String.valueOf(request.page())));
            for (var record : records) {
                HistoryLine line = HistoryLine.of(record, HistoryCommand::nameOf, wallet::format, ZoneId.systemDefault());
                messenger.send(sender, "history.entry", Placeholder.unparsed("time", line.time()),
                        Placeholder.unparsed("ic", line.ic()), Placeholder.unparsed("kind", line.kind()),
                        Placeholder.unparsed("who", line.who()), Placeholder.parsed("business", line.business()),
                        Placeholder.unparsed("items", line.items()), Placeholder.unparsed("money", line.money()));
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return startingWith(HistoryRequest.FILTERS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("kind")) {
            return startingWith(HistoryRequest.kindNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return startingWith(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> startingWith(List<String> options, String typed) {
        String start = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(start)).toList();
    }

    /** @return the UUID of a player who has played here, without asking Mojang */
    private static Optional<UUID> playerNamed(String name) {
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name)).map(OfflinePlayer::getUniqueId);
    }

    private static String nameOf(UUID player) {
        String name = Bukkit.getOfflinePlayer(player).getName();
        return name != null ? name : player.toString();
    }
}
