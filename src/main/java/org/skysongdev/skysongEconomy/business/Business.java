package org.skysongdev.skysongEconomy.business;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A business: a protected block, owned by one character, worked by others.
 * <p>
 * A business is identified by its {@link #id()}, which never changes, so its block can be moved
 * or broken without losing it. Players see its {@link #displayName()} instead.
 * <p>
 * This class only holds state and checks its own invariants. Rules that need other parts of the
 * plugin (tier limits, unique blocks, saving) live in {@link BusinessService}. Not thread-safe:
 * use it on the main thread.
 */
public final class Business {
    /** The lowest and highest tiers a business can have. */
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private final String id;
    private final String typeId;
    private String displayName;
    private int tier;
    private CharacterRef owner;
    private @Nullable BlockPosition position;
    private @Nullable String shiftMessage;
    private final List<Hire> workers;
    private final List<SkillPick> picks;

    private Business(BusinessSnapshot snapshot) {
        this.id = snapshot.id();
        this.typeId = snapshot.typeId();
        this.displayName = snapshot.displayName();
        this.tier = checkTier(snapshot.tier());
        this.owner = snapshot.owner();
        this.position = snapshot.position();
        this.shiftMessage = snapshot.shiftMessage();
        this.workers = new ArrayList<>(snapshot.workers());
        this.workers.sort(Comparator.comparing(Hire::hiredAt));
        this.picks = new ArrayList<>(snapshot.picks());
        this.picks.sort(Comparator.comparing(SkillPick::pickedAt));
    }

    /** Creates a new tier 1 business whose display name is its id, with no workers. */
    public static Business create(String id, String typeId, CharacterRef owner, @Nullable BlockPosition position) {
        return new Business(new BusinessSnapshot(id, id, typeId, MIN_TIER, owner, position, null, List.of(), List.of()));
    }

    /** Rebuilds a business from storage. */
    public static Business restore(BusinessSnapshot snapshot) {
        return new Business(snapshot);
    }

    public BusinessSnapshot snapshot() {
        return new BusinessSnapshot(id, displayName, typeId, tier, owner, position, shiftMessage, workers, picks);
    }

    public String id() {
        return id;
    }

    public String typeId() {
        return typeId;
    }

    public String displayName() {
        return displayName;
    }

    void rename(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public int tier() {
        return tier;
    }

    void setTier(int tier) {
        this.tier = checkTier(tier);
    }

    public CharacterRef owner() {
        return owner;
    }

    /** @return whether the character owns this business */
    public boolean isOwner(CharacterRef character) {
        return owner.equals(character);
    }

    void setOwner(CharacterRef owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        workers.removeIf(hire -> hire.character().equals(owner));
    }

    /** @return the business block, empty while it has none (for example after staff broke it) */
    public Optional<BlockPosition> position() {
        return Optional.ofNullable(position);
    }

    void setPosition(@Nullable BlockPosition position) {
        this.position = position;
    }

    /** @return the owner's shift message, empty when the server default is used */
    public Optional<String> shiftMessage() {
        return Optional.ofNullable(shiftMessage);
    }

    void setShiftMessage(@Nullable String shiftMessage) {
        this.shiftMessage = shiftMessage;
    }

    /** @return the workers in hire order, oldest first */
    public List<Hire> workers() {
        return List.copyOf(workers);
    }

    /** @return whether the character works here; the owner is not a worker */
    public boolean isWorker(CharacterRef character) {
        return workers.stream().anyMatch(hire -> hire.character().equals(character));
    }

    /** @return whether the character is the owner or a worker */
    public boolean isMember(CharacterRef character) {
        return isOwner(character) || isWorker(character);
    }

    void addWorker(Hire hire) {
        if (isMember(hire.character())) {
            throw new IllegalStateException(hire.character() + " is already a member of " + id);
        }
        workers.add(hire);
        workers.sort(Comparator.comparing(Hire::hiredAt));
    }

    boolean removeWorker(CharacterRef character) {
        return workers.removeIf(hire -> hire.character().equals(character));
    }

    /**
     * Removes the newest hires until at most {@code limit} remain.
     *
     * @return the removed hires, newest first
     */
    List<Hire> trimWorkersTo(int limit) {
        List<Hire> removed = new ArrayList<>();
        while (workers.size() > Math.max(0, limit)) {
            removed.add(workers.remove(workers.size() - 1));
        }
        return removed;
    }

    /** @return the skill picks, oldest first; there are at most {@link #tier()} */
    public List<SkillPick> picks() {
        return List.copyOf(picks);
    }

    /** @return how many times the skill was picked */
    public int pickCount(String skillId) {
        return (int) picks.stream().filter(pick -> pick.skillId().equals(skillId)).count();
    }

    /** @return how many picks the business has left to make */
    public int freePicks() {
        return Math.max(0, tier - picks.size());
    }

    void addPick(SkillPick pick) {
        if (freePicks() == 0) {
            throw new IllegalStateException(id + " has no free skill pick");
        }
        picks.add(pick);
        picks.sort(Comparator.comparing(SkillPick::pickedAt));
    }

    /** Removes the pick at {@code index} (0 = oldest). */
    SkillPick removePick(int index) {
        return picks.remove(index);
    }

    /**
     * Removes the newest picks until at most {@code limit} remain.
     *
     * @return the removed picks, newest first
     */
    List<SkillPick> trimPicksTo(int limit) {
        List<SkillPick> removed = new ArrayList<>();
        while (picks.size() > Math.max(0, limit)) {
            removed.add(picks.remove(picks.size() - 1));
        }
        return removed;
    }

    private static int checkTier(int tier) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            throw new IllegalArgumentException("tier must be between " + MIN_TIER + " and " + MAX_TIER + ": " + tier);
        }
        return tier;
    }
}
