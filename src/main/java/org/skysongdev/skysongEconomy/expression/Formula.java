package org.skysongdev.skysongEconomy.expression;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Arithmetic with placeholders, for example {@code {base} * 1.3} or {@code 3 + {tier}}.
 * <p>
 * It allows numbers, {@code + - * /}, parentheses, unary minus and {@code {placeholders}}. Which
 * placeholders exist is up to the caller ({@link Placeholders}), and so are their values when
 * the formula is evaluated. A formula is parsed once and evaluated many times, and the result is
 * exact: the caller truncates it where that matters.
 */
public final class Formula {
    /** Precision of division, far beyond any price or amount. */
    private static final MathContext DIVISION = MathContext.DECIMAL64;

    private final String text;
    private final Node root;
    private final Set<String> placeholders;

    private Formula(String text, Node root, Set<String> placeholders) {
        this.text = text;
        this.root = root;
        this.placeholders = Collections.unmodifiableSet(placeholders);
    }

    /**
     * @param text         the formula as written
     * @param placeholders which placeholders it may use
     * @throws FormulaException {@link FormulaException.Kind#SYNTAX} when the text isn't a valid formula
     */
    public static Formula parse(String text, Placeholders placeholders) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(placeholders, "placeholders");
        Parser parser = new Parser(text.strip(), placeholders);
        Node root = parser.parse();
        return new Formula(text.strip(), root, parser.used);
    }

    /**
     * @param values the value of each placeholder, asked by its canonical name; may throw to
     *               refuse one, and that exception is passed on
     * @return the exact result
     * @throws FormulaException {@link FormulaException.Kind#DIVISION_BY_ZERO} when it divides by zero
     */
    public BigDecimal evaluate(Function<String, BigDecimal> values) {
        return root.value(new Evaluation(text, values));
    }

    /** @return the canonical names of the placeholders it uses, in the order they first appear */
    public Set<String> placeholders() {
        return placeholders;
    }

    /** @return the formula as written, without surrounding spaces */
    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Formula formula && formula.text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }

    /** What one evaluation needs: the placeholder values, and the text for error messages. */
    private record Evaluation(String text, Function<String, BigDecimal> values) {
    }

    /** One part of a parsed formula. */
    private interface Node {
        BigDecimal value(Evaluation evaluation);
    }

    private record Constant(BigDecimal number) implements Node {
        @Override
        public BigDecimal value(Evaluation evaluation) {
            return number;
        }
    }

    private record Placeholder(String name) implements Node {
        @Override
        public BigDecimal value(Evaluation evaluation) {
            return evaluation.values().apply(name);
        }
    }

    private record Negate(Node operand) implements Node {
        @Override
        public BigDecimal value(Evaluation evaluation) {
            return operand.value(evaluation).negate();
        }
    }

    private record Binary(char operator, Node left, Node right) implements Node {
        @Override
        public BigDecimal value(Evaluation evaluation) {
            BigDecimal a = left.value(evaluation);
            BigDecimal b = right.value(evaluation);
            return switch (operator) {
                case '+' -> a.add(b);
                case '-' -> a.subtract(b);
                case '*' -> a.multiply(b);
                default -> {
                    if (b.signum() == 0) {
                        throw new FormulaException(FormulaException.Kind.DIVISION_BY_ZERO, evaluation.text(),
                                "divides by zero");
                    }
                    yield a.divide(b, DIVISION);
                }
            };
        }
    }

    /**
     * Recursive descent over:
     * <pre>
     * expression = term (("+" | "-") term)*
     * term       = factor (("*" | "/") factor)*
     * factor     = ("-" | "+") factor | number | "(" expression ")" | "{" placeholder "}"
     * </pre>
     */
    private static final class Parser {
        private final String text;
        private final Placeholders placeholders;
        private final Set<String> used = new LinkedHashSet<>();
        private int position;

        Parser(String text, Placeholders placeholders) {
            this.text = text;
            this.placeholders = placeholders;
        }

        Node parse() {
            if (text.isEmpty()) {
                throw error("is empty");
            }
            Node node = expression();
            skipSpaces();
            if (position < text.length()) {
                throw error("has '" + text.charAt(position) + "' where an operator was expected");
            }
            return node;
        }

        private Node expression() {
            Node node = term();
            while (true) {
                skipSpaces();
                if (accept('+')) {
                    node = new Binary('+', node, term());
                } else if (accept('-')) {
                    node = new Binary('-', node, term());
                } else {
                    return node;
                }
            }
        }

        private Node term() {
            Node node = factor();
            while (true) {
                skipSpaces();
                if (accept('*')) {
                    node = new Binary('*', node, factor());
                } else if (accept('/')) {
                    node = new Binary('/', node, factor());
                } else {
                    return node;
                }
            }
        }

        private Node factor() {
            skipSpaces();
            if (accept('-')) {
                return new Negate(factor());
            }
            if (accept('+')) {
                return factor();
            }
            if (accept('(')) {
                Node inner = expression();
                skipSpaces();
                if (!accept(')')) {
                    throw error("is missing a ')'");
                }
                return inner;
            }
            if (peek() == '{') {
                return placeholder();
            }
            if (Character.isDigit(peek()) || peek() == '.') {
                return number();
            }
            throw position >= text.length() ? error("ends too early") : error("has '" + peek() + "' where a value was expected");
        }

        private Node placeholder() {
            int close = text.indexOf('}', position);
            if (close < 0) {
                throw error("has a '{' without '}'");
            }
            String written = text.substring(position + 1, close).strip();
            position = close + 1;
            String name;
            try {
                name = placeholders.canonical(written);
            } catch (Placeholders.Unknown unknown) {
                throw error(unknown.getMessage());
            }
            used.add(name);
            return new Placeholder(name);
        }

        private Node number() {
            int start = position;
            while (position < text.length() && (Character.isDigit(peek()) || peek() == '.')) {
                position++;
            }
            String digits = text.substring(start, position);
            try {
                return new Constant(new BigDecimal(digits));
            } catch (NumberFormatException e) {
                throw error("has '" + digits + "', which isn't a number");
            }
        }

        private boolean accept(char expected) {
            if (peek() == expected) {
                position++;
                return true;
            }
            return false;
        }

        private char peek() {
            return position < text.length() ? text.charAt(position) : '\0';
        }

        private void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private FormulaException error(String problem) {
            return new FormulaException(FormulaException.Kind.SYNTAX, text, problem);
        }
    }
}
