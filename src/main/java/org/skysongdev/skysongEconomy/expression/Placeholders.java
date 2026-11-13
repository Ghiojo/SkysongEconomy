package org.skysongdev.skysongEconomy.expression;

/**
 * Says which placeholders one kind of {@link Formula} accepts. Prices accept {@code {base}} and
 * {@code {item:<material_id>}}; amounts accept {@code {level}} and {@code {tier}}.
 */
@FunctionalInterface
public interface Placeholders {
    /**
     * Checks one placeholder while a formula is parsed.
     *
     * @param written what is between the braces, without surrounding spaces
     * @return the name the formula keeps for it, and later asks its value by
     * @throws Unknown when this kind of formula has no such placeholder
     */
    String canonical(String written);

    /** A placeholder this kind of formula doesn't accept. */
    final class Unknown extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /**
         * @param problem what is wrong, completing "the formula ...", for example
         *                {@code has {cost}: use {base} or {item:<material_id>}}
         */
        public Unknown(String problem) {
            super(problem);
        }
    }
}
