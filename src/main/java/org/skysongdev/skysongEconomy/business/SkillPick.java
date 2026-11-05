package org.skysongdev.skysongEconomy.business;

import java.time.Instant;
import java.util.Objects;

/**
 * One skill dedication of a business. A business gets one pick per tier, may pick the same skill
 * more than once, and keeps its picks unless staff clear them. The pick date decides which picks
 * go first when a downgrade leaves too few: the newest.
 *
 * @param skillId the SkysongStats skill id
 */
public record SkillPick(String skillId, Instant pickedAt) {
    public SkillPick {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(pickedAt, "pickedAt");
    }
}
