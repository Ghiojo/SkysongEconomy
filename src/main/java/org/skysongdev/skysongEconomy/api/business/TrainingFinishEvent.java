package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A training session finished and its rewards were given. Fired on the main thread. A deleted
 * business finishes its sessions at once.
 */
public final class TrainingFinishEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable BusinessInfo business;
    private final TrainingInfo session;
    private final List<SkillTrainedInfo> skills;

    public TrainingFinishEvent(@Nullable BusinessInfo business, @NotNull TrainingInfo session,
                               @NotNull List<SkillTrainedInfo> skills) {
        this.business = business;
        this.session = Objects.requireNonNull(session, "session");
        this.skills = List.copyOf(Objects.requireNonNull(skills, "skills"));
    }

    /** @return the business; empty when it no longer exists, so nothing was given */
    public @NotNull Optional<BusinessInfo> getBusiness() {
        return Optional.ofNullable(business);
    }

    /** @return the session that finished */
    public @NotNull TrainingInfo getSession() {
        return session;
    }

    /** @return what it gave, one entry per skill the business picked */
    public @NotNull List<SkillTrainedInfo> getSkills() {
        return skills;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
