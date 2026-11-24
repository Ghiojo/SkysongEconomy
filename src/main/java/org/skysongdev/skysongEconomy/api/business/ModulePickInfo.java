package org.skysongdev.skysongEconomy.api.business;

import java.time.Instant;
import java.util.Objects;

/**
 * A module a business picked for one of its skills.
 *
 * @param businessId the business
 * @param order      the pick's number within the business, counted up from 1 and never reused
 * @param skill      the skill whose pick it took
 * @param moduleId   the module, by id
 * @param pickedAt   when it was picked
 */
public record ModulePickInfo(String businessId, int order, String skill, String moduleId, Instant pickedAt) {
    public ModulePickInfo {
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(pickedAt, "pickedAt");
    }
}
