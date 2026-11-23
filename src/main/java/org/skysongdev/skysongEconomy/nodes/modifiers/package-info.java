/**
 * Calendar node modifiers: files in {@code nodes/modifiers} that change what nodes give while the
 * IC calendar is in a season, a range of dates or the festival. A modifier names the nodes it hits
 * ({@link org.skysongdev.skysongEconomy.nodes.modifiers.NodeTarget}), when it is on
 * ({@link org.skysongdev.skysongEconomy.nodes.modifiers.CalendarWindow}) and what it does: a
 * {@link org.skysongdev.skysongEconomy.loot.YieldEffect}, or {@code dormant}, which stops harvests.
 * Every modifier on at once adds up, and with the harvest's booster
 * ({@link org.skysongdev.skysongEconomy.nodes.modifiers.NodeModifiers}).
 * <p>
 * The IC date comes from {@link org.skysongdev.skysongEconomy.calendar.CalendarClock}; without
 * SkysongCalendar no modifier ever applies. No server classes, so it is unit tested.
 */
package org.skysongdev.skysongEconomy.nodes.modifiers;
