package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.bench.BenchRegistry;
import org.skysongdev.skysongEconomy.business.bench.BusinessBench;
import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business bench &lt;assign|unassign|info|list&gt;: staff tie SkysongCrafting benches to
 * businesses. Every command but {@code list} acts on the bench the staff member looks at.
 */
final class BenchCommand extends CommandGroup {
    /** Needed for every bench command. */
    static final String PERMISSION = "skysongeconomy.business.bench";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    BenchCommand(BusinessCommandSupport support, BenchRegistry benches) {
        super("bench", PERMISSION, support.messenger);
        BenchText text = new BenchText(support, benches);
        register(new Assign(text));
        register(new Unassign(text));
        register(new Info(text));
        register(new ListBenches(text));
    }

    /** What the bench commands share: the services, and how a bench and a business are named. */
    record BenchText(BusinessCommandSupport support, BenchRegistry benches) {
        /** @return the bench the player looks at; empty, with a message, when they look at none or aren't a player */
        Optional<BlockPosition> target(CommandSender sender) {
            return support.requirePlayer(sender).flatMap(support::targetBlock);
        }

        /** @return the bench's SkysongCrafting type, or a word saying it isn't known */
        String type(BlockPosition bench) {
            return benches.benchType(bench).orElseGet(() -> plain("business.bench.type-unknown"));
        }

        /** @return the business's display name; its id when it no longer exists */
        String businessName(String businessId) {
            return support.businesses.find(businessId).map(Business::displayName).orElse(businessId);
        }

        TagResolver[] describe(BusinessBench link) {
            return new TagResolver[]{
                    unparsed("type", type(link.bench())),
                    unparsed("position", link.bench().toString()),
                    unparsed("business", businessName(link.businessId())),
                    unparsed("id", link.businessId()),
                    unparsed("by", link.assignedBy()),
                    unparsed("when", TIME.format(link.assignedAt()))};
        }

        private String plain(String key) {
            return PlainTextComponentSerializer.plainText().serialize(support.messenger.render(key));
        }
    }

    /** Base of the bench commands: checks the argument count and shows refusals. */
    private abstract static class BenchSubCommand implements SubCommand {
        final BenchText text;
        private final String name;
        private final String usage;
        private final int requiredArgs;

        BenchSubCommand(BenchText text, String name, String usage, int requiredArgs) {
            this.text = text;
            this.name = name;
            this.usage = usage;
            this.requiredArgs = requiredArgs;
        }

        @Override
        public final String name() {
            return name;
        }

        @Override
        public final String usage() {
            return usage;
        }

        @Override
        public final void execute(CommandSender sender, String[] args) {
            if (args.length < requiredArgs) {
                text.support().messenger.send(sender, "command.usage",
                        Placeholder.unparsed("usage", "/se business bench " + name + " " + usage));
                return;
            }
            try {
                run(sender, args);
            } catch (RefusedException refusal) {
                text.support().messenger.send(sender, refusal);
            }
        }

        abstract void run(CommandSender sender, String[] args);

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return List.of();
        }
    }

    /** {@code assign <id>}: ties the bench to the business, moving it from another business if needed. */
    private static final class Assign extends BenchSubCommand {
        Assign(BenchText text) {
            super(text, "assign", "<id>", 1);
        }

        @Override
        void run(CommandSender sender, String[] args) {
            text.target(sender).ifPresent(bench -> {
                BenchRegistry.Assignment assignment = text.benches().assign(bench, args[0], sender.getName());
                TagResolver[] placeholders = text.describe(assignment.link());
                assignment.previous().ifPresentOrElse(
                        previous -> text.support().messenger.send(sender, "business.bench.moved",
                                TagResolver.resolver(placeholders),
                                unparsed("previous", text.businessName(previous.businessId()))),
                        () -> text.support().messenger.send(sender, "business.bench.assigned", placeholders));
            });
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return args.length == 1 ? text.support().businessIds(args[0]) : List.of();
        }
    }

    /** {@code unassign}: unties the bench from its business. */
    private static final class Unassign extends BenchSubCommand {
        Unassign(BenchText text) {
            super(text, "unassign", "", 0);
        }

        @Override
        void run(CommandSender sender, String[] args) {
            text.target(sender).ifPresent(bench -> text.benches().unassign(bench, sender.getName()).ifPresentOrElse(
                    link -> text.support().messenger.send(sender, "business.bench.unassigned", text.describe(link)),
                    () -> text.support().messenger.send(sender, "business.bench.not-assigned")));
        }
    }

    /** {@code info}: the bench's type, its business, who tied it and when. */
    private static final class Info extends BenchSubCommand {
        Info(BenchText text) {
            super(text, "info", "", 0);
        }

        @Override
        void run(CommandSender sender, String[] args) {
            text.target(sender).ifPresent(bench -> {
                Optional<BusinessBench> link = text.benches().at(bench);
                if (link.isPresent()) {
                    text.support().messenger.send(sender, "business.bench.info", text.describe(link.get()));
                } else if (text.benches().benchType(bench).isPresent()) {
                    text.support().messenger.send(sender, "business.bench.info-free",
                            unparsed("type", text.type(bench)), unparsed("position", bench.toString()));
                } else {
                    text.support().messenger.send(sender, "business.bench.not-a-bench", unparsed("block", bench.toString()));
                }
            });
        }
    }

    /** {@code list <id>}: the business's benches. Works from the console too. */
    private static final class ListBenches extends BenchSubCommand {
        ListBenches(BenchText text) {
            super(text, "list", "<id>", 1);
        }

        @Override
        void run(CommandSender sender, String[] args) {
            Business business = text.support().businesses.require(args[0]);
            List<BusinessBench> links = text.benches().of(business.id());
            if (links.isEmpty()) {
                text.support().messenger.send(sender, "business.bench.list-empty", unparsed("business", business.displayName()));
                return;
            }
            text.support().messenger.send(sender, "business.bench.list-header",
                    unparsed("business", business.displayName()), unparsed("count", String.valueOf(links.size())));
            links.forEach(link -> text.support().messenger.send(sender, "business.bench.list-entry", text.describe(link)));
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return args.length == 1 ? text.support().businessIds(args[0]) : List.of();
        }
    }
}
