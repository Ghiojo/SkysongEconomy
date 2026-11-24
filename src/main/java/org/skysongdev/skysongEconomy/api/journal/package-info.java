/**
 * The economy journal: one {@link org.skysongdev.skysongEconomy.api.journal.EconomyRecord} for every
 * movement of money or goods SkysongEconomy makes, kept forever. Plugins that keep books or statistics
 * (such as SkysongPolitics and SkysongMetrics) listen to
 * {@link org.skysongdev.skysongEconomy.api.journal.EconomyRecordedEvent}. These types only change in
 * ways that keep code compiling: new record components, methods, enum constants and record kinds may
 * be added, existing ones are not removed or renamed.
 */
package org.skysongdev.skysongEconomy.api.journal;
