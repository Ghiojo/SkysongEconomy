package org.skysongdev.skysongEconomy.api.piers;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The receiving piers, and the way for other plugins to manage them. Get it with
 * {@code Bukkit.getServicesManager().load(Piers.class)}. Use it on the main thread only.
 * <p>
 * Piers come from two places:
 * <ul>
 *     <li><b>{@code piers.yml}</b>: their name, icon and description belong to the file, and they can't
 *     be removed here. Everything else ({@link #setOpen open}, {@link #setData data},
 *     {@link #setLossChance loss chance}, {@link #setLossModifier loss modifiers}) can be changed,
 *     and those changes are saved in the database on top of the file.</li>
 *     <li><b>Plugins</b>, with {@link #create}: saved in the database and fully managed here.</li>
 * </ul>
 * Every change is saved at once, used by the next order or collection, and followed by a
 * {@link PierChangeEvent}. A refused change throws {@link PierException}.
 */
public interface Piers {
    /** @return every pier: those from {@code piers.yml} in file order, then plugin piers in creation order */
    List<Pier> all();

    /** @return the pier with that id, in any case; empty when there is none */
    Optional<Pier> find(String id);

    /** @return the piers new orders may pick */
    default List<Pier> open() {
        return all().stream().filter(Pier::open).toList();
    }

    /**
     * @return every shipment sent to that pier that hasn't been collected yet, on its way or
     *         waiting, soonest arrival first; empty when businesses are disabled
     */
    List<Shipment> inbound(String pierId);

    /**
     * Creates a pier owned by a plugin. It starts open, with no data, a 0% loss chance and no modifiers.
     *
     * @param owner the creating plugin's name ({@code plugin.getName()}), kept as the pier's {@link Pier#source()}
     * @throws PierException {@code piers.invalid-id} or {@code piers.exists}
     */
    Pier create(String owner, String id, PierDetails details);

    /**
     * Changes a plugin pier's name, icon and description.
     *
     * @throws PierException {@code piers.unknown} or {@code piers.from-config} (edit the file instead)
     */
    Pier update(String id, PierDetails details);

    /**
     * Removes a plugin pier and its modifiers. Shipments already sent there keep its id and still arrive.
     *
     * @throws PierException {@code piers.unknown} or {@code piers.from-config} (delete it from the file instead)
     */
    void remove(String id);

    /**
     * Opens or closes a pier. A closed pier can't be picked for new orders; shipments already on
     * their way still arrive there.
     *
     * @throws PierException {@code piers.unknown}
     */
    Pier setOpen(String id, boolean open);

    /**
     * Sets or removes one free-form value. On a pier from {@code piers.yml}, a value set here wins
     * over the file's, and removing it shows the file's again.
     *
     * @param value {@code null} removes it
     * @throws PierException {@code piers.unknown}
     */
    Pier setData(String id, String key, @Nullable String value);

    /**
     * Sets the base loss chance. On a pier from {@code piers.yml} it wins over the file's.
     *
     * @param percent 0 to 100
     * @throws PierException {@code piers.unknown} or {@code piers.invalid-chance}
     */
    Pier setLossChance(String id, BigDecimal percent);

    /**
     * Adds a loss modifier, or replaces the pier's modifier with the same id.
     *
     * @throws PierException {@code piers.unknown}
     */
    Pier setLossModifier(String id, LossModifier modifier);

    /**
     * Removes the pier's loss modifier with that id. One written in {@code piers.yml} can't be
     * removed, only replaced.
     *
     * @return whether it had one set through here (or a command), which is now gone
     * @throws PierException {@code piers.unknown}
     */
    boolean removeLossModifier(String id, String modifierId);

    /** What pier and modifier ids may hold. */
    Pattern ID = Pattern.compile("[a-z0-9_-]{1,64}");

    /**
     * @return the id, stripped and in lower case
     * @throws PierException {@code piers.invalid-id} when it is empty, too long or holds other characters
     */
    static String checkId(String id, String what) {
        Objects.requireNonNull(id, what);
        String normalized = id.strip().toLowerCase(Locale.ROOT);
        if (!ID.matcher(normalized).matches()) {
            throw new PierException("piers.invalid-id", Map.of("id", id, "what", what));
        }
        return normalized;
    }
}
