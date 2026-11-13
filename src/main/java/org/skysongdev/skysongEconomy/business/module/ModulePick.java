package org.skysongdev.skysongEconomy.business.module;

import java.time.Instant;

/**
 * One module a business picked.
 *
 * @param businessId the business
 * @param order      the pick's number within the business, counted up from 1 and never reused
 *                   while the business has a higher one, so a higher number is a newer pick
 * @param skill      the skill whose pick it took
 * @param moduleId   the module, by id
 * @param pickedAt   when it was picked
 */
public record ModulePick(String businessId, int order, String skill, String moduleId, Instant pickedAt) {
}
