/**
 * The IC calendar as the rest of the plugin sees it: today's season and date, and the names of the
 * seasons and months, so config files can name them.
 * <p>
 * SkysongCalendar is optional. This package has no SkysongCalendar or server classes: it reaches
 * the calendar through {@link org.skysongdev.skysongEconomy.calendar.CalendarClock}, which
 * {@code integration/skysongcalendar/SkysongCalendarAdapter} implements over the SkysongCalendar
 * API, and {@link org.skysongdev.skysongEconomy.calendar.CalendarClock#NONE} stands in when it is
 * missing. Season and month names are never written in SkysongEconomy: they always come from the
 * installed SkysongCalendar.
 */
package org.skysongdev.skysongEconomy.calendar;
