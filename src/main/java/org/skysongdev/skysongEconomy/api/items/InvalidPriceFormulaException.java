package org.skysongdev.skysongEconomy.api.items;

import java.io.Serial;
import java.util.Objects;

/** A text that isn't a valid price formula, thrown by {@link PriceFormulas#parse}. */
public final class InvalidPriceFormulaException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String formula;
    private final String problem;

    /**
     * @param formula the text that was parsed
     * @param problem what is wrong, completing "the formula ...", for example
     *                {@code has {cost}: use {base} or {item:<material_id>}}
     */
    public InvalidPriceFormulaException(String formula, String problem) {
        super("The price formula '" + Objects.requireNonNull(formula, "formula") + "' "
                + Objects.requireNonNull(problem, "problem"));
        this.formula = formula;
        this.problem = problem;
    }

    /** @return the text that was parsed */
    public String formula() {
        return formula;
    }

    /** @return what is wrong, completing "the formula ..." */
    public String problem() {
        return problem;
    }
}
