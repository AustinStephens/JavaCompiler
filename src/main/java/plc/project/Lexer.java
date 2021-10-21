package plc.project;

import java.awt.print.PrinterAbortException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        ArrayList<Token> tokens = new ArrayList<Token>();

        while(chars.has(0)){
            Token t = lexToken();
            if(t != null) tokens.add(t);
        }

        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if(chars.has(0)) {
            if (peek("\\s") || peek("\b")) {
                lexEscape();
                return lexToken();
            } else if (peek("\\+") || peek("\\-")) {
                if (peek("[\\+\\-]", "\\d")) return lexNumber();
                else return lexOperator();
            } else if (peek("\\d")) {
                return lexNumber();
            } else if (peek("\"")) {
                return lexString();
            } else if (peek("\'")) {
                return lexCharacter();
            } else if (peek("[a-zA-Z_]")) {
                return lexIdentifier();
            } else {
                return lexOperator();
            }
        } else return null;
    }

    public Token lexIdentifier() {
        chars.advance();
        while(peek("\\w|-")) {
            chars.advance();
        }

        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        chars.advance();
        boolean alreadyDec = false;
        while(peek("\\d|\\.")) {
            if(peek("\\.")) {
                if(!peek("\\.", "\\d")) return chars.emit((alreadyDec? Token.Type.DECIMAL : Token.Type.INTEGER));
                else if(alreadyDec) return chars.emit(Token.Type.DECIMAL);
                alreadyDec = true;
            }
            chars.advance();
        }
        return chars.emit((alreadyDec? Token.Type.DECIMAL : Token.Type.INTEGER));
    }

    public Token lexCharacter() {
        if(!peek("\'", "\'")) {
            if(chars.has(2)) {
                if (chars.get(2) == '\'' && chars.get(1) != '\\') {
                    advanceN(3);
                    return chars.emit(Token.Type.CHARACTER);
                } else if (peek("\'", "\n|\r")) {
                    throw new ParseException("language does not support multi line character definitions, use escape characters", chars.index + 1);
                } else {
                    if(chars.has(3)) {
                        if (chars.get(3) == '\'') {
                            if (peek("\'", "\\\\", "[bnrtf\'\"\\\\]", "\'")) {
                                advanceN(4);
                                return chars.emit(Token.Type.CHARACTER);
                            } else {
                                throw new ParseException("invalid escape character", chars.index + 1);
                            }
                        }
                    } else {
                        throw new ParseException("no closing quote", chars.index+3);
                    }
                }
            } else {
                throw new ParseException("no closing quote", chars.index+2);
            }
        }

        throw new ParseException("character literal cannot be empty", chars.index+1);
    }

    public Token lexString() {
        chars.advance();
        while(!peek("\"") && chars.has(0)) {
            if(peek("\n|\r")) throw new ParseException("language does not support multi line string definitions, use escape characters", chars.index);
            if(peek("\\\\")) {
                if(!peek("\\\\", "[bnrtf\'\"\\\\]|000B")) throw new ParseException("invalid escape character", chars.index+1);
                chars.advance();
            }
            chars.advance();
        }
        if(chars.has(0)) {
            chars.advance();
            return chars.emit(Token.Type.STRING);
        } else {
            throw new ParseException("never ended the string literal with a double quote", chars.index);
        }
    }

    public void lexEscape() {
        chars.advance();
        chars.skip();
    }

    public Token lexOperator() {
        if(peek("\\s") || peek("\b") || !chars.has(0)) {
            lexEscape();
            System.out.println("hey");
            return lexToken();
        }
        if(peek("[<>!=]","="))
            chars.advance();

        if(!peek("\\s")) chars.advance();
        return chars.emit(Token.Type.OPERATOR);
    }

    private void advanceN(int n) {
        for(int i = 0; i < n; i ++) {
            chars.advance();
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i< patterns.length; i++) {
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }

        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
