package org.skysongdev.skysongEconomy.upkeep;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What {@code npc-upkeep.yml} says.
 *
 * @param checkEvery how often cycles that are due are settled
 * @param profiles   the profiles by id
 * @param templates  the profile id each SkysongNPCs template gives, by lower-case template id;
 *                   every value is a key of {@code profiles}
 * @param nameLineHeight how far above the top of an unmaintained NPC's head its red line sits, in blocks
 */
public record UpkeepSettings(Duration checkEvery, Map<String, UpkeepProfile> profiles, Map<String, String> templates,
                             double nameLineHeight) {
    /** Settled once a minute unless the file says otherwise. */
    public static final Duration DEFAULT_CHECK_EVERY = Duration.ofSeconds(60);

    /** Just above the head, under where the name floats. */
    public static final double DEFAULT_NAME_LINE_HEIGHT = 0.2;

    /** No profiles: nothing has an upkeep. */
    public static final UpkeepSettings NONE = new UpkeepSettings(DEFAULT_CHECK_EVERY, Map.of(), Map.of(),
            DEFAULT_NAME_LINE_HEIGHT);

    /** Settings with the default name line height. */
    public UpkeepSettings(Duration checkEvery, Map<String, UpkeepProfile> profiles, Map<String, String> templates) {
        this(checkEvery, profiles, templates, DEFAULT_NAME_LINE_HEIGHT);
    }

    public UpkeepSettings {
        Objects.requireNonNull(checkEvery, "checkEvery");
        profiles = Map.copyOf(profiles);
        templates = Map.copyOf(templates);
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            if (!profiles.containsKey(entry.getValue())) {
                throw new IllegalArgumentException("Template " + entry.getKey() + " names the unknown profile " + entry.getValue());
            }
        }
    }

    /** @return the profile with that id */
    public Optional<UpkeepProfile> profile(String id) {
        return Optional.ofNullable(profiles.get(id));
    }

    /** @return the profile NPCs made from that template get; empty when the template is not listed */
    public Optional<UpkeepProfile> forTemplate(String template) {
        return Optional.ofNullable(templates.get(template.toLowerCase(Locale.ROOT))).map(profiles::get);
    }
}
