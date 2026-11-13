package org.skysongdev.skysongEconomy.business.npc;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * What gives a business an NPC slot. Every business NPC fills a slot of exactly one source, and
 * each source has its own number of slots, its own SkysongNPCs template and its own settings.
 * <p>
 * A source is a skill listed in {@code businesses/npcs.yml}, or a picked module with an
 * {@code npc-slots} effect. It is saved as {@link #key()}, such as {@code skill:martial} or
 * {@code module:kennel}.
 *
 * @param kind what kind of thing gives the slots: {@link #SKILL} or {@link #MODULE}
 * @param id   which one: the skill's id, or the module's
 */
public record SlotSource(String kind, String id) {
    /** The kind of a skill's slots. */
    public static final String SKILL = "skill";
    /** The kind of the slots a picked module gives; the id is the module's. */
    public static final String MODULE = "module";

    private static final char SEPARATOR = ':';

    public SlotSource {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        if (kind.isEmpty() || id.isEmpty() || kind.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Not a slot source: " + kind + SEPARATOR + id);
        }
    }

    /** @return the slots a skill's NPC settings give */
    public static SlotSource skill(String skillId) {
        return new SlotSource(SKILL, skillId.toLowerCase(Locale.ROOT));
    }

    /** @return the slots a module's {@code npc-slots} effect gives */
    public static SlotSource module(String moduleId) {
        return new SlotSource(MODULE, moduleId.toLowerCase(Locale.ROOT));
    }

    /**
     * @param key a saved source, such as {@code skill:martial}
     * @return the source, empty when the text is not written that way
     */
    public static Optional<SlotSource> parse(String key) {
        int separator = key.indexOf(SEPARATOR);
        if (separator <= 0 || separator == key.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(new SlotSource(key.substring(0, separator), key.substring(separator + 1)));
    }

    public boolean isSkill() {
        return SKILL.equals(kind);
    }

    public boolean isModule() {
        return MODULE.equals(kind);
    }

    /** @return the text this source is saved as, such as {@code skill:martial} */
    public String key() {
        return kind + SEPARATOR + id;
    }

    @Override
    public String toString() {
        return key();
    }
}
