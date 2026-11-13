package org.skysongdev.skysongEconomy.expression;

/**
 * A {@link Formula} couldn't be read or worked out. {@link #problem()} completes the sentence
 * "the formula ...", for example {@code is missing a ')'}, so each use can wrap it in its own
 * message.
 */
public final class FormulaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** What went wrong. */
    public enum Kind {
        /** The text isn't a valid formula. */
        SYNTAX,
        /** Working it out divided by zero. */
        DIVISION_BY_ZERO
    }

    private final Kind kind;
    private final String formula;
    private final String problem;

    /**
     * @param kind    what went wrong
     * @param formula the formula as written
     * @param problem what is wrong with it, completing "the formula ..."
     */
    public FormulaException(Kind kind, String formula, String problem) {
        super("'" + formula + "' " + problem);
        this.kind = kind;
        this.formula = formula;
        this.problem = problem;
    }

    /** @return what went wrong */
    public Kind kind() {
        return kind;
    }

    /** @return the formula as written */
    public String formula() {
        return formula;
    }

    /** @return what is wrong with it, completing "the formula ..." */
    public String problem() {
        return problem;
    }
}
