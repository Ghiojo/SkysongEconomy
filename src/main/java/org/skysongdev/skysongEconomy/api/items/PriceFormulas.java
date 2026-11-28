package org.skysongdev.skysongEconomy.api.items;

/**
 * Reads price formulas the way SkysongEconomy's own ledgers do, so other plugins can let their
 * config files price things with {@code {item:<material_id>}} and {@code {base}} without copying
 * the parser. Registered with Bukkit's services manager while the plugin is enabled:
 *
 * <pre>{@code
 * PriceFormulas formulas = Bukkit.getServicesManager().load(PriceFormulas.class);
 * PriceFormula price = formulas.parse("{item:steel_ingot} * 1.2 + {base}");
 * Optional<BigDecimal> now = price.value(new BigDecimal("3"));
 * }</pre>
 *
 * A formula allows numbers, {@code + - * /}, parentheses, a leading minus, {@code {base}} (a value
 * the caller supplies) and {@code {item:<material_id>}} (that item's base value in the price
 * database, {@code /se ledger}).
 * <p>
 * Call it on the main thread. Methods are only ever added, never removed or changed, within a
 * major version.
 */
public interface PriceFormulas {
    /**
     * Parses a formula once, to be worked out as often as needed. Item references are not
     * checked here: an item without a base value only makes {@link PriceFormula#value} empty.
     *
     * @param text the formula as written, such as {@code {base} * 0.8}
     * @return the parsed formula
     * @throws InvalidPriceFormulaException when the text isn't a valid formula
     */
    PriceFormula parse(String text);
}
