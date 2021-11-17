package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        ArrayList<Ast.Field> fields = new ArrayList<Ast.Field>();
        ArrayList<Ast.Method> methods = new ArrayList<Ast.Method>();

        boolean flag = true;
        while(flag) {
            if(peek("LET")) {
                Ast.Field field = parseField();
                fields.add(field);
            } else {
                flag = false;
            }
        }

        flag = true;
        while(flag) {
            if (peek("DEF")) {
                Ast.Method method = parseMethod();
                methods.add(method);
            } else if (tokens.has(0)){
                exceptionHelper("Expected field or method");
            } else {
                flag = false;
            }
        }

        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        // 'LET' identifier ':' identifier ('=' expression)? ';'
        match("LET");

        if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Identifier.");
        String name = tokens.get(-1).getLiteral();

        if(!match(":")) exceptionHelper("Expected Colon.");

        if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Type Name.");
        String type = tokens.get(-1).getLiteral();

        Optional<Ast.Expr> value = Optional.empty();

        if(match("=")) {
            Ast.Expr expr = parseExpression();
            value = Optional.of(expr);
        }

        if(!match(";")) exceptionHelper("Missing semicolon");

        return new Ast.Field(name, type, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        // 'DEF' identifier '(' (identifier ':' identifier (',' identifier ':' identifier)*)? ')' (':' identifier)? 'DO' statement* 'END'

        match("DEF");

        if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Identifier.");
        String name = tokens.get(-1).getLiteral();

        if(!match("(")) exceptionHelper("Missing opening parenthesis.");

        ArrayList<String> params = new ArrayList<String>();
        ArrayList<String> paramTypes = new ArrayList<String>();

        if(match(Token.Type.IDENTIFIER)) {
            params.add(tokens.get(-1).getLiteral());
            if(!match(":")) exceptionHelper("Expected colon.");
            if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Type Name");
            paramTypes.add(tokens.get(-1).getLiteral());
            while(match(",")) {
                if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Identifier.");
                params.add(tokens.get(-1).getLiteral());
                if(!match(":")) exceptionHelper("Expected colon.");
                if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Type Name");
                paramTypes.add(tokens.get(-1).getLiteral());
            }
        }

        if(!match(")")) exceptionHelper("Missing closing parenthesis");

        String returnType = null;
        if(match(":")) {
            if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Return Type Name.");
            returnType = tokens.get(-1).getLiteral();
        }

        if(!match("DO")) exceptionHelper("Missing DO keyword");

        ArrayList<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();
        boolean flag = true;

        while(flag) {
            if(peek("END") || !tokens.has(0)) {
                flag = false;
            } else {
                Ast.Stmt stmt = parseStatement();
                statements.add(stmt);
            }
        }

        if(!match("END")) exceptionHelper("Expected END keyword.");

        Optional<String> type = Optional.empty();
        if(returnType != null)
            type = Optional.of(returnType);
        return new Ast.Method(name, params, paramTypes, type, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if(peek("LET")) {
            return parseDeclarationStatement();
        } else if(peek("IF")) {
            return parseIfStatement();
        } else if(peek("FOR")) {
            return parseForStatement();
        } else if(peek("WHILE")) {
            return parseWhileStatement();
        } else if(peek("RETURN")) {
            return parseReturnStatement();
        } else {
            Ast.Expr expr = parseExpression();
            if(match("=")) {
                Ast.Expr expr2 = parseExpression();
                if(match(";"))
                    return new Ast.Stmt.Assignment(expr, expr2);
                else {
                    exceptionHelper("Missing semicolon.");
                }

            }

            if(match(";"))
                return new Ast.Stmt.Expression(expr);
            else {
                exceptionHelper("Missing semicolon.");
            }
        }

        throw new ParseException("temp", -1);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        // 'LET' identifier (':' identifier)? ('=' expression)? ';'
        match("LET");

        if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Identifier.");
        String name = tokens.get(-1).getLiteral();

        Optional<String> type = Optional.empty();
        if(match(":")) {
            if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Type Name.");
            type = Optional.of(tokens.get(-1).getLiteral());
        }

        Optional<Ast.Expr> value = Optional.empty();
        if(match("=")) {
            Ast.Expr expr = parseExpression();
            value = Optional.of(expr);
        }

        if(!match(";")) exceptionHelper("Missing semicolon");

        return new Ast.Stmt.Declaration(name, type, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        // 'IF' expression 'DO' statement* ('ELSE' statement*)? 'END'
        match("IF");

        Ast.Expr expr = parseExpression();

        if(!match("DO")) exceptionHelper("Expected DO keyword");

        boolean flag = true;
        ArrayList<Ast.Stmt> thenStatements = new ArrayList<Ast.Stmt>();

        while(flag) {
            if(peek("ELSE") || peek("END") || !tokens.has(0)) {
                flag = false;
            } else {
                Ast.Stmt stmt = parseStatement();
                thenStatements.add(stmt);
            }
        }

        ArrayList<Ast.Stmt> elseStatements = new ArrayList<Ast.Stmt>();
        flag = true;

        if(match("ELSE")) {
            while(flag) {
                if(peek("END") || !tokens.has(0)) {
                    flag = false;
                } else {
                    Ast.Stmt stmt = parseStatement();
                    elseStatements.add(stmt);
                }
            }
        }

        if(!match("END")) exceptionHelper("Expected END keyword.");

        return new Ast.Stmt.If(expr, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        // 'FOR' identifier 'IN' expression 'DO' statement* 'END'
        match("FOR");

        if(!match(Token.Type.IDENTIFIER)) exceptionHelper("Expected Identifier.");
        String name = tokens.get(-1).getLiteral();

        if(!match("IN")) exceptionHelper("Expected IN keyword");
        Ast.Expr expr = parseExpression();

        if(!match("DO")) exceptionHelper("Expected DO keyword.");

        boolean flag = true;
        ArrayList<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();

        while(flag) {
            if(peek("END") || !tokens.has(0)) {
                flag = false;
            } else {
                Ast.Stmt stmt = parseStatement();
                statements.add(stmt);
            }
        }

        if(!match("END")) exceptionHelper("Expected END keyword.");

        return new Ast.Stmt.For(name, expr, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        // 'WHILE' expression 'DO' statement* 'END'
        match("WHILE");

        Ast.Expr expr = parseExpression();

        if(!match("DO")) exceptionHelper("Expected DO keyword.");

        boolean flag = true;
        ArrayList<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();

        while(flag) {
            if(peek("END") || !tokens.has(0)) {
                flag = false;
            } else {
                Ast.Stmt stmt = parseStatement();
                statements.add(stmt);
            }
        }

        if(!match("END")) exceptionHelper("Expected END keyword.");

        return new Ast.Stmt.While(expr, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        // 'RETURN' expression ';'
        match("RETURN");

        Ast.Expr expr = parseExpression();

        if(!match(";")) exceptionHelper("Missing semicolon");

        return new Ast.Stmt.Return(expr);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr expr = parseEqualityExpression();
        boolean flag = true;

        while(flag) {
            if (match("AND")) {
                Ast.Expr expr2 = parseEqualityExpression();
                expr =  new Ast.Expr.Binary("AND", expr, expr2);
            } else if (match("OR")) {
                Ast.Expr expr2 = parseEqualityExpression();
                expr = new Ast.Expr.Binary("OR", expr, expr2);
            } else {
                flag = false;
            }
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr expr = parseAdditiveExpression();
        boolean flag = true;

        while(flag) {
            if (match("<")) {
                expr = equalityHelper("<", expr);
            } else if (match("<=")) {
                expr = equalityHelper("<=", expr);
            } else if (match(">")) {
                expr = equalityHelper(">", expr);
            } else if (match(">=")) {
                expr = equalityHelper(">=", expr);
            } else if (match("==")) {
                expr = equalityHelper("==", expr);
            } else if (match("!=")) {
                expr = equalityHelper("!=", expr);
            } else {
                flag = false;
            }
        }
        return expr;
    }

    private Ast.Expr equalityHelper(String operator, Ast.Expr expr) throws ParseException {
        Ast.Expr expr2 = parseAdditiveExpression();
        return new Ast.Expr.Binary(operator, expr, expr2);
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr expr = parseMultiplicativeExpression();
        boolean flag = true;

        while(flag) {
            if (match("+")) {
                Ast.Expr expr2 = parseMultiplicativeExpression();
                expr = new Ast.Expr.Binary("+", expr, expr2);
            } else if (match("-")) {
                Ast.Expr expr2 = parseMultiplicativeExpression();
                expr = new Ast.Expr.Binary("-", expr, expr2);
            } else {
                flag = false;
            }
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expr = parseSecondaryExpression();
        boolean flag = true;

        while(flag) {
            if (match("*")) {
                Ast.Expr expr2 = parseSecondaryExpression();
                expr = new Ast.Expr.Binary("*", expr, expr2);
            } else if (match("/")) {
                Ast.Expr expr2 = parseSecondaryExpression();
                expr = new Ast.Expr.Binary("/", expr, expr2);
            } else {
                flag = false;
            }
        }
        return expr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr expr = parsePrimaryExpression();

        while(match(".")) { // check for dot operator
            if(match(Token.Type.IDENTIFIER)) {  //make sure its an identifier
                String name = tokens.get(-1).getLiteral(); //identifier name
                if(match("(")) {  //if its a method call
                    if(match(")")) {
                        expr = new Ast.Expr.Function(Optional.of(expr), name, Arrays.asList());
                    } else {
                        ArrayList<Ast.Expr> params = new ArrayList<>();
                        do {
                            params.add(parseExpression());
                        } while (match(",")); //do once, then keep going if theres a comma
                        if (match(")")) {//make sure we check if theres a closing bracket else throw exception
                            expr = new Ast.Expr.Function(Optional.of(expr), name, params);
                        } else {
                            exceptionHelper("Expected closing parenthesis");
                        }
                    }
                } else { // if its a variable
                    expr = new Ast.Expr.Access(Optional.of(expr), name); //makes the entire expresion before the new expression if
                    // theres multiple dot operators
                }
            } else {
                exceptionHelper("Invalid dot operator use");
            }
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if(match("TRUE")) {
            return new Ast.Expr.Literal(true);
        } else if(match("FALSE")) {
            return new Ast.Expr.Literal(false);
        } else if(match("NIL")) {
            return new Ast.Expr.Literal(null);
        } else if(match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if(match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if(match(Token.Type.STRING)) {
            String str = tokens.get(-1).getLiteral();
            str = str.replaceAll("\\\\b", "\b");
            str = str.replaceAll("\\\\r", "\r");
            str = str.replaceAll("\\\\t", "\t");
            str = str.replaceAll("\\\\n", "\n");
            str = str.replaceAll("\\\\f", "\f");
            str = str.replaceAll("\\\\000B", "\013");
            str = str.replaceAll("\\\\\'", "\'");
            str = str.replaceAll("\\\\\"", "\"");
            str = str.replaceAll("\\\\\\\\", "\\");
            return new Ast.Expr.Literal(str.substring(1, str.length()-1));
        } else if(match(Token.Type.CHARACTER)) {
            String str = tokens.get(-1).getLiteral();
            str = str.replaceAll("\\\\b", "\b");
            str = str.replaceAll("\\\\r", "\r");
            str = str.replaceAll("\\\\t", "\t");
            str = str.replaceAll("\\\\n", "\n");
            str = str.replaceAll("\\\\f", "\f");
            str = str.replaceAll("\\\\000B", "\013");
            str = str.replaceAll("\\\\\'", "\'");
            str = str.replaceAll("\\\\\"", "\"");
            str = str.replaceAll("\\\\\\\\", "\\");
            return new Ast.Expr.Literal(new Character(str.charAt(1)));
        } else if(match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if(match("(")) {  //if its a method call
                if(match(")")) {
                    return new Ast.Expr.Function(Optional.empty(), name, Arrays.asList());
                } else {
                    ArrayList<Ast.Expr> params = new ArrayList<>();
                    do {
                        params.add(parseExpression());
                    } while (match(",")); //do once, then keep going if theres a comma
                    if (match(")")) {//make sure we check if theres a closing bracket else throw exception
                        return new Ast.Expr.Function(Optional.empty(), name, params);
                    } else {
                        exceptionHelper("Expected closing parenthesis");
                    }
                }
            }
            return new Ast.Expr.Access(Optional.empty(), name);
            //obj.method()
        } else if(match("(")) {
            Ast.Expr expr = parseExpression();
            if(!match(")")) {
                exceptionHelper("Expected closing parenthesis");
            }
            return new Ast.Expr.Group(expr);
        } else {
            exceptionHelper("Invalid Primary Expression.");
            return null;
        }
    }

    private void exceptionHelper(String str) throws ParseException {
        if(tokens.has(0))
            throw new ParseException(str, tokens.get(0).getIndex());
        else
            throw new ParseException(str, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if(!tokens.has(i)) {
                return false;
            } else if(patterns[i] instanceof Token.Type) {
                if(patterns[i] != tokens.get(i).getType())
                    return false;
            } else if(patterns[i] instanceof String) {
                if(!patterns[i].equals(tokens.get(i).getLiteral()))
                    return false;
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }

        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
