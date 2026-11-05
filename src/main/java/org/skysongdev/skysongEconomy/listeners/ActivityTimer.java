package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.business.training.SkillTrained;
import org.skysongdev.skysongEconomy.business.training.TrainingResult;
import org.skysongdev.skysongEconomy.business.training.TrainingService;
import org.skysongdev.skysongEconomy.character.ProficiencyTraining;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.integration.cmi.AfkCheck;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.time.Duration;
import java.util.UUID;

/**
 * Every few seconds, adds that time to the shift or training session of every online player
 * who isn't AFK, and tells them when it ends. Offline time never counts because offline
 * players aren't ticked.
 */
public final class ActivityTimer implements Runnable {
    /** How often the timer runs; an activity can end up to this much late. */
    public static final Duration INTERVAL = Duration.ofSeconds(10);

    private static final long TICKS_PER_SECOND = 20;

    private final Plugin plugin;
    private final BusinessService businesses;
    private final ShiftService shifts;
    private final TrainingService training;
    private final SkillCatalog skills;
    private final AfkCheck afk;
    private final Messenger messenger;

    public ActivityTimer(Plugin plugin, BusinessService businesses, ShiftService shifts, TrainingService training,
                         SkillCatalog skills, AfkCheck afk, Messenger messenger) {
        this.plugin = plugin;
        this.businesses = businesses;
        this.shifts = shifts;
        this.training = training;
        this.skills = skills;
        this.afk = afk;
        this.messenger = messenger;
    }

    /** Starts the timer on the main thread. */
    public BukkitTask start() {
        long ticks = INTERVAL.toSeconds() * TICKS_PER_SECOND;
        return plugin.getServer().getScheduler().runTaskTimer(plugin, this, ticks, ticks);
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            boolean busy = shifts.active(id).isPresent() || training.active(id).isPresent();
            if (!busy || afk.isAfk(player)) {
                continue;
            }
            shifts.work(id, INTERVAL).ifPresent(shift ->
                    messenger.send(player, "business.shift.finished", Placeholder.unparsed("business", name(shift.businessId()))));
            training.work(id, INTERVAL).ifPresent(result -> tellTrained(player, result));
        }
    }

    private void tellTrained(Player player, TrainingResult result) {
        messenger.send(player, "business.training.finished",
                Placeholder.unparsed("business", name(result.session().businessId())));
        for (SkillTrained skill : result.skills()) {
            String key = skill.proficiencyResult().map(ActivityTimer::rewardKey).orElse("business.training.reward.xp-only");
            messenger.send(player, key, Placeholder.parsed("skill", skills.displayName(skill.skillId())),
                    Placeholder.unparsed("xp", String.valueOf(skill.xp())));
        }
    }

    private String name(String businessId) {
        return businesses.find(businessId).map(Business::displayName).orElse(businessId);
    }

    private static String rewardKey(ProficiencyTraining outcome) {
        return switch (outcome) {
            case TRAINED -> "business.training.reward.trained";
            case NO_FOCUS -> "business.training.reward.no-focus";
            case OTHER_SKILL -> "business.training.reward.other-skill";
            case AT_MAX_TIER -> "business.training.reward.at-max-tier";
            case UNAVAILABLE -> "business.training.reward.unavailable";
        };
    }
}
