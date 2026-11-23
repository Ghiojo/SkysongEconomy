/**
 * What other plugins can compile against to work with import piers: {@link
 * org.skysongdev.skysongEconomy.api.piers.Piers} to read and manage piers and see the
 * {@link org.skysongdev.skysongEconomy.api.piers.Shipment shipments} heading to them, and the events
 * about them, such as {@link org.skysongdev.skysongEconomy.api.piers.ShipmentOrderEvent}.
 * <p>
 * Every import a business orders is sent to one receiving pier. A pier has a loss chance: the
 * percent chance that a shipment collected there is lost. Loss modifiers raise or lower it, and
 * can be limited to shipments with one tag, such as {@code contraband}.
 * <p>
 * Add SkysongEconomy as {@code compileOnly 'com.github.Ghiojo:SkysongEconomy:<tag>'} and as a
 * {@code softdepend} (or {@code depend}) in plugin.yml. For example, guards stationed at a pier
 * catching contraband:
 * <pre>{@code
 * Piers piers = Bukkit.getServicesManager().load(Piers.class);
 * piers.setLossModifier("harbour", new LossModifier("guards", BigDecimal.valueOf(5L * guardCount), false,
 *         Optional.of("contraband")));
 * // and when the guards leave:
 * piers.removeLossModifier("harbour", "guards");
 * }</pre>
 * These types only change in ways that keep such code compiling: new record components or
 * methods may be added, existing ones are not removed or renamed.
 */
package org.skysongdev.skysongEconomy.api.piers;
