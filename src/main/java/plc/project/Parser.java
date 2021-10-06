package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

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
        Ast.Global global = null;

        if (peek("LIST")){
            global = parseList();
        }else if (peek("VAR")){
            global = parseMutable();
        }else if (peek("VAL")){
            global = parseImmutable();
        }

        if(!match(";")){
            throw new ParseException("Missing Semicolon",-1);
            //TODO: token error index to character
        }

        if(global==null){
            throw new ParseException("Anything, anything at all",0);
        }

        return global;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        List<Ast.Expression> elements = new ArrayList<>();//list to construct list expression

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
        List<Ast.Statement> statements;

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
        List<Ast.Statement> statements = new ArrayList<>();

        while(peek("LET")||peek("SWITCH")||peek("IF")||
                peek("WHILE")||peek("RETURN")||peek("NIL")
                ||peek("TRUE")||peek("FALSE")||peek(Token.Type.INTEGER)
                ||peek(Token.Type.DECIMAL)||peek(Token.Type.CHARACTER)||peek(Token.Type.STRING)
                ||peek("(")||peek(Token.Type.IDENTIFIER)){
            statements.add(parseStatement());
        }

        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Statement stmnt = null;

       if (peek("LET")){
           stmnt = parseDeclarationStatement();
       }

       else {
           Ast.Expression expr = parseExpression();
           if (peek("=")) {
               match("=");
               Ast.Expression expr1 = parseExpression();
               stmnt = new Ast.Statement.Assignment(expr,expr1);
           }
           else {
               stmnt = new Ast.Statement.Expression(expr);
           }
       }

       /*if (!match(";")){//parse expression should stop before ;
           throw new ParseException("Expected semicolon in Statement.",-1);
           //TODO: handle actual character index instead of -1
       }*///dont need this here, needs to be handled individually

       return stmnt;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");

        if(!match(Token.Type.IDENTIFIER)){
            throw new ParseException("Expected Identifier",-1);
            //TODO : HANDLE actual character index instead of -1
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();

        if (match("=")){
            value = Optional.of(parseExpression());
        }

        if(!match(";")){
            throw new ParseException("Expected semicolon.", -1);
            //TODO: handle actual character index
        }

        return new Ast.Statement.Declaration(name, value);

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
        Ast.Expression expr = null;

        if (peek("NIL")){
            expr = new Ast.Expression.Literal(null);
        } else if (peek("TRUE")){
            expr = new Ast.Expression.Literal(Boolean.TRUE);
        } else if (peek("FALSE")){
            expr = new Ast.Expression.Literal(Boolean.FALSE);
        }else if (peek(Token.Type.INTEGER)){//INTEGER
            match(Token.Type.INTEGER);
            BigInteger nextInt = new BigInteger(tokens.get(-1).getLiteral());
            expr = new Ast.Expression.Literal(nextInt);
        }else if (peek(Token.Type.DECIMAL)){
            match(Token.Type.DECIMAL);
            BigDecimal nextdec = new BigDecimal(tokens.get(-1).getLiteral());
            expr = new Ast.Expression.Literal(nextdec);
        }else if (peek(Token.Type.CHARACTER)){
            match(Token.Type.CHARACTER);
            String toNoQuotes = tokens.get(-1).getLiteral();
            toNoQuotes = toNoQuotes.replace("\'","");
            toNoQuotes = toNoQuotes.replace("\\b","\b");//replacement of escape characters
            toNoQuotes = toNoQuotes.replace("\\n","\n");
            toNoQuotes = toNoQuotes.replace("\\r","\r");
            toNoQuotes = toNoQuotes.replace("\\t","\t");

            Character tempChar = toNoQuotes.charAt(0);

            expr = new Ast.Expression.Literal(tempChar);
        }else if (peek(Token.Type.STRING)){
            match(Token.Type.STRING);
            String toNoQuotes = tokens.get(-1).getLiteral();
            toNoQuotes = toNoQuotes.replace("\"","");
            toNoQuotes = toNoQuotes.replace("\\b","\b");//replacement of escape characters
            toNoQuotes = toNoQuotes.replace("\\n","\n");
            toNoQuotes = toNoQuotes.replace("\\r","\r");
            toNoQuotes = toNoQuotes.replace("\\t","\t");

            expr = new Ast.Expression.Literal(toNoQuotes);
        }
        //group,
        else if(peek("(")){
            match("(");
            Ast.Expression expr1 = parseExpression();

            expr = new Ast.Expression.Group(expr1);

            if (!match(")")){
                throw new ParseException("Expected closing parentheses 1",-1);
                //TODO: specific character instead of last token
            }
        }
        // function, access
        else if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
            String name = tokens.get(-1).getLiteral();
            List<Ast.Expression> argumentList = new ArrayList<>();

            if (peek("(")){
                match("(");

                if(!peek(")")) {
                    argumentList.add(parseExpression());
                }
                // while loop for multiple arguments
                while(peek(",")){
                    match(",");
                    argumentList.add(parseExpression());
                }

                expr = new Ast.Expression.Function(name,argumentList);

                if (!match(")")){
                    throw new ParseException("Expected closing parenthesis",-1);
                    //TODO: specific character of error instead of last token
                }

            }
            else if (peek("[")){
                match("[");

                expr = new Ast.Expression.Access(Optional.of(parseExpression()),name);
                if (!match("]")){
                    throw new ParseException("Expected closing bracket",-1);
                    //TODO: specific character of error instead of last token
                }
            }
            else expr = new Ast.Expression.Access(Optional.empty(),name);

        }
        return expr;
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