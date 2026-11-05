package org.skysongdev.skysongEconomy.prices;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A price written as arithmetic, for example {@code {base} * 1.3} or
 * {@code {item:steel_ingot} * 2 + 1}.
 * <p>
 * It allows numbers, {@code + - * /}, parentheses, unary minus, {@code {base}} (the base value
 * of what is being priced) and {@code {item:<material_id>}} (another item's base value).
 * Expressions are parsed once and evaluated many times. The result is exact: truncate it with
 * {@link PriceSettings#truncate} where players see or pay it.
 */
public final class PriceExpression {
    /** Precision of division, far beyond any price. */
    private static final MathContext DIVISION = MathContext.DECIMAL64;

    private final String text;
    private final Node root;
    private final Set<String> items;
    private final boolean usesBase;

    private PriceExpression(String text, Node root, Set<String> items, boolean usesBase) {
        this.text = text;
        this.root = root;
        this.items = Collections.unmodifiableSet(items);
        this.usesBase = usesBase;
    }

    /**
     * @throws PriceException {@code prices.invalid-expression} when the text isn't a valid expression
     */
    public static PriceExpression parse(String text) {
        Objects.requireNonNull(text, "text");
        Parser parser = new Parser(text.strip());
        Node root = parser.parse();
        return new PriceExpression(text.strip(), root, parser.items, parser.usesBase);
    }

    /** @return an expression that is just this number */
    public static PriceExpression of(BigDecimal value) {
        return parse(value.toPlainString());
    }

    /**
     * @throws PriceException when a value it needs is missing, or it divides by zero
     */
    public BigDecimal evaluate(PriceContext context) {
        return root.value(context);
    }

    /** @return the material ids its {@code {item:...}} references name, in order */
    public Set<String> items() {
        return items;
    }

    /** @return whether it uses {@code {base}} */
    public boolean usesBase() {
        return usesBase;
    }

    /** @return the expression as written, without surrounding spaces */
    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PriceExpression expression && expression.text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }

    /** One part of a parsed expression. */
    private interface Node {
        BigDecimal value(PriceContext context);
    }

    private record Constant(BigDecimal number) implements Node {
        @Override
        public BigDecimal value(PriceContext context) {
            return number;
        }
    }

    private record Base() implements Node {
        @Override
        public BigDecimal value(PriceContext context) {
            return context.base();
        }
    }

    private record Item(String materialId) implements Node {
        @Override
        public BigDecimal value(PriceContext context) {
            return context.item(materialId);
        }
    }

    private record Negate(Node operand) implements Node {
        @Override
        public BigDecimal value(PriceContext context) {
            return operand.value(context).negate();
        }
    }

    private record Binary(char operator, Node left, Node right) implements Node {
        @Override
        public BigDecimal value(PriceContext context) {
            BigDecimal a = left.value(context);
            BigDecimal b = right.value(context);
            return switch (operator) {
                case '+' -> a.add(b);
                case '-' -> a.subtract(b);
                case '*' -> a.multiply(b);
                default -> {
                    if (b.signum() == 0) {
                        throw new PriceException("prices.division-by-zero", Map.of());
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
     * factor     = ("-" | "+") factor | number | "(" expression ")" | "{base}" | "{item:" id "}"
     * </pre>
     */
    private static final class Parser {
        private final String text;
        private int position;
        private final Set<String> items = new LinkedHashSet<>();
        private boolean usesBase;

        Parser(String text) {
            this.text = text;
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
            String inside = text.substring(position + 1, close).strip();
            position = close + 1;
            if (inside.equals("base")) {
                usesBase = true;
                return new Base();
            }
            if (inside.startsWith("item:")) {
                String materialId = inside.substring("item:".length()).strip();
                if (materialId.isEmpty()) {
                    throw error("has {item:} without a material id");
                }
                items.add(materialId);
                return new Item(materialId);
            }
            throw error("has {" + inside + "}: use {base} or {item:<material_id>}");
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

        private PriceException error(String problem) {
            return new PriceException("prices.invalid-expression", Map.of("expression", text, "problem", problem));
        }
    }
}
