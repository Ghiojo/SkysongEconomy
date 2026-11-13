/**
 * Values in config files that follow how far a business has come.
 * <p>
 * A business has two numbers a value can depend on
 * ({@link org.skysongdev.skysongEconomy.business.scale.BusinessScale}): the <em>level</em> of one
 * skill, which is how many of the business's skill picks are that skill, and the business's
 * overall <em>tier</em>. An {@link org.skysongdev.skysongEconomy.business.scale.Amount} is a
 * whole number written as a fixed number, as a formula such as {@code 3 + {level}}, or as a
 * table such as {@code {by: tier, 1: 1, 3: 2, 5: 3}}.
 * {@link org.skysongdev.skysongEconomy.business.scale.Steps} is that table for any kind of
 * value, and {@link org.skysongdev.skysongEconomy.business.scale.SkillSections} reads the
 * {@code skills:} map of a functionality's file, which says which skills have the functionality
 * and with what settings.
 * <p>
 * This package has no server dependencies, so all of it is unit tested.
 */
package org.skysongdev.skysongEconomy.business.scale;
