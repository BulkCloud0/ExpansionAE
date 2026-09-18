package dev.bulkcloud.expansionae;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class TagExpression {
    private enum TokenType { TAG, OPERATOR, LPAREN, RPAREN }

    private enum Operator {
        NOT('!', 3, true),
        AND('&', 2, false),
        XOR('^', 1, false),
        OR('|', 0, false);

        final char symbol;
        final int precedence;
        final boolean rightAssociative;

        Operator(char symbol, int precedence, boolean rightAssociative) {
            this.symbol = symbol;
            this.precedence = precedence;
            this.rightAssociative = rightAssociative;
        }

        static Operator of(char c) {
            for (Operator op : values()) {
                if (op.symbol == c) return op;
            }
            return null;
        }
    }

    private static final class Token {
        final TokenType type;
        final String value;
        final Operator operator;

        Token(TokenType type, String value, Operator operator) {
            this.type = type;
            this.value = value;
            this.operator = operator;
        }

        static Token tag(String value) { return new Token(TokenType.TAG, value, null); }
        static Token op(Operator value) { return new Token(TokenType.OPERATOR, null, value); }
        static Token lparen() { return new Token(TokenType.LPAREN, null, null); }
        static Token rparen() { return new Token(TokenType.RPAREN, null, null); }
    }

    private TagExpression() {
    }

    static Predicate<Set<String>> compile(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return tags -> false;
        }
        String expression = raw.replace("&&", "&").replace("||", "|");
        final Queue<Token> rpn;
        try {
            rpn = toRpn(tokenize(expression));
        } catch (RuntimeException error) {
            return tags -> false;
        }
        return tags -> {
            try {
                return evaluate(rpn, tags);
            } catch (RuntimeException error) {
                return false;
            }
        };
    }

    private static List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder tag = new StringBuilder();
        boolean expectingOperand = true;
        int openParens = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) continue;

            Operator op = Operator.of(c);
            if (c == '(') {
                if (!expectingOperand) throw new IllegalArgumentException("Unexpected (");
                flush(tag, tokens);
                tokens.add(Token.lparen());
                openParens++;
                expectingOperand = true;
            } else if (c == ')') {
                flush(tag, tokens);
                if (expectingOperand || openParens <= 0) throw new IllegalArgumentException("Unexpected )");
                tokens.add(Token.rparen());
                openParens--;
                expectingOperand = false;
            } else if (op != null) {
                if (op == Operator.NOT && expectingOperand) {
                    flush(tag, tokens);
                    tokens.add(Token.op(op));
                } else if (op != Operator.NOT && !expectingOperand) {
                    flush(tag, tokens);
                    tokens.add(Token.op(op));
                    expectingOperand = true;
                } else {
                    throw new IllegalArgumentException("Unexpected operator");
                }
            } else {
                if (!expectingOperand && tag.length() == 0) {
                    throw new IllegalArgumentException("Missing operator");
                }
                tag.append(c);
                expectingOperand = false;
            }
        }

        flush(tag, tokens);
        if (tokens.isEmpty() || openParens != 0 || expectingOperand) {
            throw new IllegalArgumentException("Incomplete expression");
        }
        return tokens;
    }

    private static void flush(StringBuilder tag, List<Token> tokens) {
        if (tag.length() > 0) {
            tokens.add(Token.tag(tag.toString()));
            tag.setLength(0);
        }
    }

    private static Queue<Token> toRpn(List<Token> tokens) {
        Queue<Token> output = new LinkedList<>();
        Deque<Token> operators = new ArrayDeque<>();

        for (Token token : tokens) {
            switch (token.type) {
                case TAG:
                    output.add(token);
                    break;
                case OPERATOR:
                    while (!operators.isEmpty() && operators.peek().type == TokenType.OPERATOR) {
                        Operator current = token.operator;
                        Operator top = operators.peek().operator;
                        boolean pop = (!current.rightAssociative && current.precedence <= top.precedence)
                                || (current.rightAssociative && current.precedence < top.precedence);
                        if (!pop) break;
                        output.add(operators.pop());
                    }
                    operators.push(token);
                    break;
                case LPAREN:
                    operators.push(token);
                    break;
                case RPAREN:
                    boolean found = false;
                    while (!operators.isEmpty()) {
                        Token top = operators.pop();
                        if (top.type == TokenType.LPAREN) {
                            found = true;
                            break;
                        }
                        output.add(top);
                    }
                    if (!found) throw new IllegalArgumentException("Mismatched parentheses");
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        while (!operators.isEmpty()) {
            Token token = operators.pop();
            if (token.type == TokenType.LPAREN || token.type == TokenType.RPAREN) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(token);
        }
        return output;
    }

    private static boolean evaluate(Queue<Token> source, Set<String> tags) {
        Deque<Boolean> values = new ArrayDeque<>();
        for (Token token : new LinkedList<>(source)) {
            if (token.type == TokenType.TAG) {
                boolean match = false;
                for (String actual : tags) {
                    if (wildcard(token.value, actual)) {
                        match = true;
                        break;
                    }
                }
                values.push(match);
            } else if (token.type == TokenType.OPERATOR) {
                if (token.operator == Operator.NOT) {
                    if (values.isEmpty()) throw new IllegalArgumentException();
                    values.push(!values.pop());
                } else {
                    if (values.size() < 2) throw new IllegalArgumentException();
                    boolean right = values.pop();
                    boolean left = values.pop();
                    switch (token.operator) {
                        case AND:
                            values.push(left && right);
                            break;
                        case OR:
                            values.push(left || right);
                            break;
                        case XOR:
                            values.push(left ^ right);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
            }
        }
        if (values.size() != 1) throw new IllegalArgumentException();
        return values.pop();
    }

    private static boolean wildcard(String pattern, String text) {
        if ("*".equals(pattern) || pattern.equals(text)) return true;
        String[] pieces = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0) regex.append(".*");
            regex.append(Pattern.quote(pieces[i]));
        }
        regex.append("$");
        return text.matches(regex.toString());
    }
}
