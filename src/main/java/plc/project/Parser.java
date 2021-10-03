package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.emptyList;

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
        List<Ast.Global> globalList = new ArrayList<>();
        List<Ast.Function> functionList = new ArrayList<>();

        while (peek("LIST")||peek("VAR")||peek("VAL")){
            globalList.add(parseGlobal());
        }
        while(peek("FUN")){
            functionList.add(parseFunction());
        }
        return new Ast.Source(globalList,functionList);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("LIST")){
            return parseList();
        }else if (peek("VAR")){
            return parseMutable();
        }else if (peek("VAL")){
            return parseImmutable();
        }else throw new ParseException("stopped in parse Global",tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        List<Ast.Expression> elements = new ArrayList<Ast.Expression>();//list to construct list expression

        match("LIST", Token.Type.IDENTIFIER);
        String name = tokens.get(-1).getLiteral();
        match("=","[");//peek first for error checking?
        elements.add(parseExpression());

        //while loop to add rest of expressions (',' expression)*
        while(peek(",")){
            match(",");
            elements.add(parseExpression());
        }

        match("]");

        Ast.Expression.PlcList plc = new Ast.Expression.PlcList(elements);
        Optional<Ast.Expression> list = Optional.of(plc);

        return new Ast.Global(name,Boolean.FALSE,list);//global to return with elements in constructor
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        match(Token.Type.IDENTIFIER);

        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> arg = Optional.empty();

        if(peek("=")){
            match("=");
            Ast.Expression expr = parseExpression();
             arg = Optional.of(expr);
        }

        return new Ast.Global(name,Boolean.TRUE,arg);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        match(Token.Type.IDENTIFIER);

        String name = tokens.get(-1).getLiteral();

        match("=");

        Ast.Expression expr = parseExpression();
        Optional<Ast.Expression> arg = Optional.of(expr);

        return new Ast.Global(name,Boolean.FALSE,arg);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        match(Token.Type.IDENTIFIER);

        String name = tokens.get(-1).getLiteral();

        List<String> arguments = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();

        match("(");

        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
            arguments.add(tokens.get(-1).getLiteral());//first argument

            while(peek(",")){
                match(",");
                match(Token.Type.IDENTIFIER);
                arguments.add(tokens.get(-1).getLiteral());
            }
        }

        match(")");
        match("DO");
        statements = parseBlock();
        match("END");

        return new Ast.Function(name,arguments,statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek(Token.Type.IDENTIFIER)) {
            while (tokens.has(0)) {
                if ((tokens.get(1).getType() == Token.Type.OPERATOR) && (tokens.get(1).getLiteral().matches("\\("))) {
                    return new Ast.Statement.Expression(parseExpression());
                }
                else if ((tokens.get(1).getType() == Token.Type.OPERATOR) && (tokens.get(1).getLiteral().matches("="))) {
                    return new Ast.Statement.Assignment(parseExpression(),parseExpression());
                }
            }
        }
        Ast.Statement.Expression a = new Ast.Statement.Expression( new Ast.Expression.Function(tokens.get(0).getLiteral(), Arrays.asList()));
        return a;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expr = parseComparisonExpression();

        while(peek("&&")||peek("||")){
            if (peek("&&")){
                match("&&");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseComparisonExpression();

                expr = new Ast.Expression.Binary(operator,expr,right);
            }
            else if (peek("||")){
                match("||");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseComparisonExpression();

                expr = new Ast.Expression.Binary(operator,expr,right);
            }
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression expr = parseAdditiveExpression();

        while(peek("<")||peek(">")||peek("==")||peek("!=")){
            if(peek("<")){
                match("<");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseAdditiveExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
            else if(peek(">")){
                match(">");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseAdditiveExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
            else if(peek("==")){
                match("==");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseAdditiveExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
            else if(peek("!=")){
                match("!=");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseAdditiveExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
        }

        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expr = parseMultiplicativeExpression();

        while(peek("+")||peek("-")){
            if (peek("+")){
                match("+");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseMultiplicativeExpression();

                expr = new Ast.Expression.Binary(operator,expr,right);
            }
            else if (peek("-")){
                match("-");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseMultiplicativeExpression();

                expr = new Ast.Expression.Binary(operator,expr,right);
            }
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expr = parsePrimaryExpression();

        while(peek("*")||peek("/")||peek("^")){
            if(peek("*")){
                match("*");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parsePrimaryExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
            else if(peek("/")){
                match("/");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parsePrimaryExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
            }
            else if(peek("^")){
                match("^");
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parsePrimaryExpression();

                expr = new Ast.Expression.Binary(operator,expr, right);
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
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        if (peek(Token.Type.INTEGER)) {
            match(Token.Type.INTEGER);
            BigInteger temp = new BigInteger(tokens.get(-1).getLiteral());
            return new Ast.Expression.Literal(temp);
        }
        else if (peek(Token.Type.STRING)) {
            match(Token.Type.STRING);
            String temp = tokens.get(-1).getLiteral();
            temp=temp.replace("\"","");
            temp=temp.replace("\\n","\n");
            return new Ast.Expression.Literal(temp);
        }
        else if (peek(Token.Type.CHARACTER)) {
            match(Token.Type.CHARACTER);
            String temp = tokens.get(-1).getLiteral();
            temp =temp.replace("'","");
            //temp= temp.substring(1,temp.length()-1);
            Character tempChar = temp.charAt(0);
            return new Ast.Expression.Literal(tempChar);
        }
        else if (peek(Token.Type.DECIMAL)) {
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }

        else if (peek((Token.Type.IDENTIFIER))) {
            match(Token.Type.IDENTIFIER);
            String temp = tokens.get(-1).getLiteral();
            if (Objects.equals(temp, "NIL") || Objects.equals(temp, "TRUE") || Objects.equals(temp, "FALSE")) {
                if (temp.equals("TRUE"))
                    return new Ast.Expression.Literal(Boolean.TRUE);
                else if (temp.equals("FALSE"))
                    return new Ast.Expression.Literal(Boolean.FALSE);
                else
                    return new Ast.Expression.Literal(null);
            }
            else {
                if (tokens.has(0)) {
                    if (tokens.get(0).getLiteral().matches("\\(")) {
                        match(Token.Type.OPERATOR);
                        if (tokens.get(0).getLiteral().matches("\\)")) {
                            return new Ast.Expression.Function(temp, Arrays.asList());
                        }
                        List<Ast.Expression> tempList= new ArrayList<>();
                        while (tokens.has(0)) {
                            tempList.add(parsePrimaryExpression());
                            match(Token.Type.OPERATOR);
                        }
                        return new Ast.Expression.Function(temp, tempList);
                    }
                    else if (tokens.get(0).getLiteral().matches("\\[")) {
                        match(Token.Type.OPERATOR);
                        return new Ast.Expression.Access(Optional.of(parsePrimaryExpression()),temp);
                    }
                    else if (tokens.get(0).getLiteral().matches("=")) {
                        match(Token.Type.OPERATOR);
                        return new Ast.Expression.Access(Optional.empty(),temp);
                    }
                }

                return new Ast.Expression.Access(Optional.empty(),temp);
            }

        }
        else if (peek(Token.Type.OPERATOR)) {
            match(Token.Type.OPERATOR);
            if (tokens.has(0)) {
                if (tokens.get(-1).getLiteral().matches("\\(")) {
                    return new Ast.Expression.Group(parseExpression());
                /*
                int i=0;
                while (true) {
                    if (tokens.get(i).getLiteral().matches("\\)")) {
                        return new Ast.Expression.Binary(tokens.get(i).getLiteral(),parsePrimaryExpression(),parsePrimaryExpression());
                    }
                    if (i==100) {
                        break;
                    }
                    i++;
                }
                 */

                }
            }
            else {
                return parsePrimaryExpression();
            }
        }

        return new Ast.Expression.Literal("idontknowwhattoputhere");
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
        for (int i =0;i<patterns.length;i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
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

        if (peek) {
            for (int i =0; i< patterns.length;i++) {
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