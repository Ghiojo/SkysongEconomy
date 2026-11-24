/**
 * What other plugins can compile against to read SkysongEconomy's businesses: the
 * {@link org.skysongdev.skysongEconomy.api.business.Businesses} service and
 * {@link org.skysongdev.skysongEconomy.api.business.BusinessInfo}.
 * <pre>{@code
 * Businesses businesses = Bukkit.getServicesManager().load(Businesses.class);
 * businesses.find("rose_smithy").ifPresent(info -> book.write(info.displayName()));
 * }</pre>
 * These types only change in ways that keep such code compiling: new record components or
 * methods may be added, existing ones are not removed or renamed.
 */
package org.skysongdev.skysongEconomy.api.business;
