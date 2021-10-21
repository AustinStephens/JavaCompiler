package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Signed Decimal", "-1.0", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Integer", "1", false),
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\\\'\'", true),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\\n\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "<=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("training newline", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                )),
                Arguments.of("is whitespace", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "X = X+5; if(x == 10) { print(\"x equals ten\\n\"); } else { return;   }   String x_s = x.toString();",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "X", 0),
                                new Token(Token.Type.OPERATOR, "=", 2),
                                new Token(Token.Type.IDENTIFIER, "X", 4),
                                new Token(Token.Type.INTEGER, "+5", 5),
                                new Token(Token.Type.OPERATOR, ";", 7),
                                new Token(Token.Type.IDENTIFIER, "if", 9),
                                new Token(Token.Type.OPERATOR, "(", 11),
                                new Token(Token.Type.IDENTIFIER, "x", 12),
                                new Token(Token.Type.OPERATOR, "==", 14),
                                new Token(Token.Type.INTEGER, "10", 17),
                                new Token(Token.Type.OPERATOR, ")", 19),
                                new Token(Token.Type.OPERATOR, "{", 21),
                                new Token(Token.Type.IDENTIFIER, "print", 23),
                                new Token(Token.Type.OPERATOR, "(", 28),
                                new Token(Token.Type.STRING, "\"x equals ten\\n\"", 29),
                                new Token(Token.Type.OPERATOR, ")", 45),
                                new Token(Token.Type.OPERATOR, ";", 46),
                                new Token(Token.Type.OPERATOR, "}", 48),
                                new Token(Token.Type.IDENTIFIER, "else", 50),
                                new Token(Token.Type.OPERATOR, "{", 55),
                                new Token(Token.Type.IDENTIFIER, "return", 57),
                                new Token(Token.Type.OPERATOR, ";", 63),
                                new Token(Token.Type.OPERATOR, "}", 67),
                                new Token(Token.Type.IDENTIFIER, "String", 71),
                                new Token(Token.Type.IDENTIFIER, "x_s", 78),
                                new Token(Token.Type.OPERATOR, "=", 82),
                                new Token(Token.Type.IDENTIFIER, "x", 84),
                                new Token(Token.Type.OPERATOR, ".", 85),
                                new Token(Token.Type.IDENTIFIER, "toString", 86),
                                new Token(Token.Type.OPERATOR, "(", 94),
                                new Token(Token.Type.OPERATOR, ")", 95),
                                new Token(Token.Type.OPERATOR, ";", 96)
                        ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\'\\n").lex());
        System.out.println(exception.getMessage());
        Assertions.assertEquals(3, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
