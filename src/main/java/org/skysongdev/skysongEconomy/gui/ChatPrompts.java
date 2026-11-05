package org.skysongdev.skysongEconomy.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player to type something in chat, for example a new business name. Their next chat
 * message is taken as the answer and not shown to anyone. Typing the cancel word, logging out or
 * waiting too long drops the question.
 */
public final class ChatPrompts implements Listener {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private final Plugin plugin;
    private final Messenger messenger;
    private final Clock clock;
    private final Map<UUID, Question> waiting = new ConcurrentHashMap<>();

    public ChatPrompts(Plugin plugin, Messenger messenger, Clock clock) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.clock = clock;
    }

    /**
     * Closes the player's menu, shows {@code questionKey} and waits for their answer.
     *
     * @param onAnswer runs on the main thread with the typed text, trimmed
     */
    public void ask(Player player, String questionKey, Consumer<String> onAnswer) {
        player.closeInventory();
        waiting.put(player.getUniqueId(), new Question(onAnswer, clock.instant().plus(TIMEOUT)));
        messenger.send(player, questionKey);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Question question = waiting.remove(player.getUniqueId());
        if (question == null) {
            return;
        }
        event.setCancelled(true);
        String answer = PlainTextComponentSerializer.plainText().serialize(event.message()).strip();
        // Chat arrives off the main thread; everything the answer touches must run on it.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (clock.instant().isAfter(question.expires())) {
                messenger.send(player, "prompt.timed-out");
            } else if (answer.equalsIgnoreCase(messenger.plainText("prompt.cancel-word"))) {
                messenger.send(player, "prompt.cancelled");
            } else {
                question.onAnswer().accept(answer);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }

    private record Question(Consumer<String> onAnswer, Instant expires) {
    }
}
