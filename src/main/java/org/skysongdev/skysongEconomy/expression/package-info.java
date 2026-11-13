/**
 * Arithmetic written in config files and commands: numbers, {@code + - * /}, parentheses and
 * named placeholders such as {@code {base}} or {@code {tier}}.
 * <p>
 * {@link org.skysongdev.skysongEconomy.expression.Formula} is the parser and evaluator. It knows
 * nothing about what a placeholder means: each use (prices, amounts that follow a business's
 * tier) says which placeholders it accepts and supplies their values.
 * <p>
 * This package has no server dependencies, so all of it is unit tested.
 */
package org.skysongdev.skysongEconomy.expression;
