package org.skysongdev.skysongEconomy.business;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Every business on the server and the rules for changing them.
 * <p>
 * Businesses are kept in memory, indexed by id and by block, and every change is passed to the
 * {@link BusinessRepository} right away. Refused actions throw a {@link BusinessException} that
 * names the message to show. Not thread-safe: use it on the main thread.
 */
public final class BusinessService {
    /** Longest display name an owner can set, in characters. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 48;
    /** Longest shift message an owner can set, in characters. */
    public static final int MAX_SHIFT_MESSAGE_LENGTH = 200;

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,64}");

    private final BusinessTypeRegistry types;
    private final BusinessTiers tiers;
    private final BusinessRepository repository;
    private final Clock clock;
    private final Map<String, Business> byId = new LinkedHashMap<>();
    private final Map<BlockPosition, Business> byPosition = new HashMap<>();
    private final List<BusinessChangeListener> listeners = new ArrayList<>();

    public BusinessService(BusinessTypeRegistry types, BusinessTiers tiers, BusinessRepository repository, Clock clock) {
        this.types = types;
        this.tiers = tiers;
        this.repository = repository;
        this.clock = clock;
    }

    /** Tells {@code listener} about every later change. */
    public void addListener(BusinessChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Loads every saved business. A business whose block another business already uses keeps
     * its data but loses its block, so staff can move it.
     *
     * @return the number of businesses loaded
     */
    public int loadAll() {
        byId.clear();
        byPosition.clear();
        for (BusinessSnapshot snapshot : repository.loadAll()) {
            Business business = Business.restore(snapshot);
            byId.put(business.id(), business);
            business.position().ifPresent(position -> {
                if (byPosition.putIfAbsent(position, business) != null) {
                    business.setPosition(null);
                    repository.save(business.snapshot());
                }
            });
        }
        List<Business> loaded = List.copyOf(byId.values());
        listeners.forEach(listener -> listener.loaded(loaded));
        return byId.size();
    }

    public Optional<Business> find(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    /** @return the business opened from that block */
    public Optional<Business> at(BlockPosition position) {
        return Optional.ofNullable(byPosition.get(position));
    }

    /** @return every business, oldest first */
    public Collection<Business> all() {
        return List.copyOf(byId.values());
    }

    /**
     * @return the business with that id
     * @throws BusinessException {@code business.not-found} when there is none
     */
    public Business require(String id) {
        return find(id).orElseThrow(() -> new BusinessException("business.not-found", Map.of("id", id)));
    }

    /**
     * Creates a tier 1 business on a block.
     *
     * @throws BusinessException when the id is invalid or taken, the type is unknown, or the block is used
     */
    public Business create(String id, String typeId, CharacterRef owner, BlockPosition position) {
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!ID.matcher(normalized).matches()) {
            throw new BusinessException("business.invalid-id", Map.of("id", id));
        }
        if (byId.containsKey(normalized)) {
            throw new BusinessException("business.id-taken", Map.of("id", normalized));
        }
        if (types.find(typeId).isEmpty()) {
            throw new BusinessException("business.unknown-type", Map.of("type", typeId));
        }
        requireFree(position);
        Business business = Business.create(normalized, typeId, owner, position);
        byId.put(normalized, business);
        byPosition.put(position, business);
        repository.save(business.snapshot());
        listeners.forEach(listener -> listener.changed(business, null));
        return business;
    }

    /** Deletes the business for good. */
    public void delete(Business business) {
        byId.remove(business.id());
        business.position().ifPresent(byPosition::remove);
        repository.delete(business.id());
        listeners.forEach(listener -> listener.deleted(business));
    }

    /**
     * Moves the business to another block.
     *
     * @throws BusinessException {@code business.block-taken} when another business uses the block
     */
    public void move(Business business, BlockPosition position) {
        if (business.position().filter(position::equals).isPresent()) {
            return;
        }
        requireFree(position);
        BlockPosition previous = business.position().orElse(null);
        business.position().ifPresent(byPosition::remove);
        business.setPosition(position);
        byPosition.put(position, business);
        save(business, previous);
    }

    /** Takes the business off its block, which happens when staff break the block. */
    public void unplace(Business business) {
        BlockPosition previous = business.position().orElse(null);
        business.position().ifPresent(byPosition::remove);
        business.setPosition(null);
        save(business, previous);
    }

    /**
     * Changes the name players see. Text only: MiniMessage tags are shown as typed.
     *
     * @throws BusinessException {@code business.invalid-name} when it is blank or too long
     */
    public void rename(Business business, String displayName) {
        String trimmed = displayName.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new BusinessException("business.invalid-name",
                    Map.of("max", String.valueOf(MAX_DISPLAY_NAME_LENGTH)));
        }
        business.rename(trimmed);
        save(business);
    }

    /**
     * Sets the owner's shift message, or goes back to the server default.
     *
     * @param message the new message, {@code null} for the default
     * @throws BusinessException {@code business.invalid-shift-message} when it is blank or too long
     */
    public void setShiftMessage(Business business, @Nullable String message) {
        String trimmed = message == null ? null : message.strip();
        if (trimmed != null && (trimmed.isEmpty() || trimmed.length() > MAX_SHIFT_MESSAGE_LENGTH)) {
            throw new BusinessException("business.invalid-shift-message",
                    Map.of("max", String.valueOf(MAX_SHIFT_MESSAGE_LENGTH)));
        }
        business.setShiftMessage(trimmed);
        save(business);
    }

    /**
     * Changes the tier. Going down removes the newest hires the new tier has no room for.
     *
     * @return the workers who were removed, newest first
     * @throws BusinessException {@code business.invalid-tier} when the tier is outside 1 to 5
     */
    public List<Hire> setTier(Business business, int tier) {
        if (tier < Business.MIN_TIER || tier > Business.MAX_TIER) {
            throw new BusinessException("business.invalid-tier", Map.of(
                    "min", String.valueOf(Business.MIN_TIER), "max", String.valueOf(Business.MAX_TIER)));
        }
        business.setTier(tier);
        List<Hire> removed = business.trimWorkersTo(tiers.get(tier).maxWorkers());
        save(business);
        return removed;
    }

    /** Gives the business to another character. If they worked there, they stop being a worker. */
    public void setOwner(Business business, CharacterRef owner) {
        business.setOwner(owner);
        save(business);
    }

    /**
     * Hires a character.
     *
     * @throws BusinessException when they already belong to the business or it is full
     */
    public Hire hire(Business business, CharacterRef character) {
        if (business.isOwner(character)) {
            throw new BusinessException("business.hire.is-owner");
        }
        if (business.isWorker(character)) {
            throw new BusinessException("business.hire.already-worker", Map.of("character", character.profile()));
        }
        int max = tiers.get(business.tier()).maxWorkers();
        if (business.workers().size() >= max) {
            throw new BusinessException("business.hire.full", Map.of("max", String.valueOf(max)));
        }
        Hire hire = new Hire(character, clock.instant());
        business.addWorker(hire);
        save(business);
        return hire;
    }

    /**
     * Removes a worker.
     *
     * @throws BusinessException {@code business.fire.not-worker} when they don't work there
     */
    public void fire(Business business, CharacterRef character) {
        if (!business.removeWorker(character)) {
            throw new BusinessException("business.fire.not-worker", Map.of("character", character.profile()));
        }
        save(business);
    }

    /** @return how many workers the business may have at its current tier */
    public int maxWorkers(Business business) {
        return tiers.get(business.tier()).maxWorkers();
    }

    private void requireFree(BlockPosition position) {
        Business other = byPosition.get(position);
        if (other != null) {
            throw new BusinessException("business.block-taken", Map.of("business", other.displayName()));
        }
    }

    private void save(Business business) {
        save(business, business.position().orElse(null));
    }

    private void save(Business business, @Nullable BlockPosition previousPosition) {
        repository.save(business.snapshot());
        listeners.forEach(listener -> listener.changed(business, previousPosition));
    }
}
