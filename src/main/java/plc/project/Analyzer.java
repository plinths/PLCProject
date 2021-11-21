package plc.project;

import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Global glob : ast.getGlobals()){
            visit( glob );
        }
        for (Ast.Function func : ast.getFunctions()){
            visit( func );
        }

        if (!(scope.lookupFunction("main",0).getReturnType().equals (Environment.Type.INTEGER))) {
            throw new RuntimeException("the main function does not have an Integer return type");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getValue().isPresent()){
            visit(ast.getValue().get());
            requireAssignable(ast.getVariable().getType(), ast.getValue().get().getType());
        }

        Environment.Type type ;

        try {
            type = Environment.getType(ast.getTypeName());
        }catch(RuntimeException e){
            type = ast.getValue().get().getType();
        }

        scope.defineVariable(ast.getName(),ast.getName(),type,Boolean.TRUE,Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        Environment.Type type;

        if (ast.getReturnTypeName().isPresent()){
            type = Environment.getType(ast.getReturnTypeName().get());
        }else {
            type = Environment.Type.NIL;
        }

        scope.defineFunction(ast.getName(),ast.getName(),ast.getFunction().getParameterTypes(),type , args->Environment.NIL );
        ast.setFunction(scope.lookupFunction(ast.getName(),ast.getParameters().size()));



        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        if (!(ast.getExpression() instanceof Ast.Expression.Function)){
            throw new RuntimeException("expression not an ast.expression.function");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()){
            visit(ast.getValue().get());
            //requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
        }

        Environment.Type type ;

        try {
            type = Environment.getType(ast.getTypeName().get());
        }catch(RuntimeException e){
            type = ast.getValue().get().getType();
        }

        ast.setVariable(scope.defineVariable(ast.getName(),ast.getName(),type,Boolean.TRUE,Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        visit(ast.getReceiver());
        visit(ast.getValue());

        if (!(ast.getReceiver() instanceof Ast.Expression.Access)){
            throw new RuntimeException("receiver is not an access expression");
        }

        requireAssignable( ast.getValue().getType(), ast.getReceiver().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit ( ast.getCondition() );
        if (!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) ||
                ( ast.getThenStatements().size()==0 )//empty then Statements
        ){
            throw new RuntimeException("condition not of type boolean or empty set of then statements");
        }

        scope = new Scope(scope);//then statements
        for ( Ast.Statement stmt : ast.getThenStatements() ){
            visit(stmt);
        }
        scope = scope.getParent();

        scope = new Scope(scope);//else statements
        for ( Ast.Statement stmt : ast.getElseStatements() ){
            visit(stmt);
        }
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());

        Environment.Type type = ast.getCondition().getType();

        for (int i = 0; i < ast.getCases().size();i++ ) {
            scope = new Scope(scope);
            visit( ast.getCases().get(i) );

            if (ast.getCases().get(i).getValue().isPresent() && !(ast.getCases().get(i).getValue().get().getType().equals(type)) ){
                throw new RuntimeException("case type does not match condition type");
            }

            if (i==ast.getCases().size()-1 && ast.getCases().get(i).getValue().isPresent()){
                throw new RuntimeException("default case should not have value" + " cases size: "+ast.getCases().size() + " case num: " + i);
            }


            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements() ) {
                visit( stmt );
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements() ) {
                visit( stmt );
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();//TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof Boolean){

            ast.setType(Environment.Type.BOOLEAN);

        } else if (ast.getLiteral() instanceof Character){

            ast.setType(Environment.Type.CHARACTER);

        } else if (ast.getLiteral() instanceof String){

            ast.setType(Environment.Type.STRING);

        } else if (ast.getLiteral()==null){

            ast.setType(Environment.Type.NIL);

        } else if (ast.getLiteral() instanceof BigInteger && ((BigInteger) ast.getLiteral()).bitCount()<32) {

            ast.setType(Environment.Type.INTEGER);

        } else if (ast.getLiteral() instanceof BigInteger){

            throw new RuntimeException("value is out of range of a Java int");

        }else if (ast.getLiteral() instanceof BigDecimal &&
                (((BigDecimal) ast.getLiteral()).doubleValue()==Double.NEGATIVE_INFINITY) || (((BigDecimal) ast.getLiteral()).doubleValue()==Double.POSITIVE_INFINITY)){

            throw new  RuntimeException("value is out of range of a Java double");

        } else if (ast.getLiteral() instanceof BigDecimal){

            ast.setType(Environment.Type.DECIMAL);

        }

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Group ast) {

        if (ast.getExpression() instanceof Ast.Expression.Binary){
            ast.setType(ast.getExpression().getType());
        }else{
            throw new RuntimeException("contained expression must be binary");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        String operator = ast.getOperator();

        visit(ast.getLeft());
        visit(ast.getRight());

        switch(operator){
            case "&&":
            case "||":
                requireAssignable(ast.getLeft().getType(),ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                if (!(ast.getLeft().getType().equals(Environment.Type.COMPARABLE))  || !(ast.getRight().getType().equals(Environment.Type.COMPARABLE)) ){
                    throw new RuntimeException("Both operands must be of type comparable");
                }
                requireAssignable(ast.getLeft().getType(),ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)){
                    ast.setType(Environment.Type.STRING);
                    break;
                }else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) || ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    requireAssignable(ast.getLeft().getType(),ast.getRight().getType());
                    ast.setType(ast.getLeft().getType());
                }
                break;
            case "-":
            case "*":
            case "/":
                if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) || ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                    requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
                    ast.setType(ast.getLeft().getType());
                }
            case "^":
                if ((ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.INTEGER))){
                    ast.setType(ast.getLeft().getType());
                }else throw new RuntimeException("^");
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        Environment.Variable var;

        if (ast.getOffset().isPresent()){
            requireAssignable(Environment.Type.INTEGER,ast.getOffset().get().getType());

        }else{
            Environment.PlcObject obj = new Environment.PlcObject(scope, scope.lookupVariable(ast.getName()));
            var = new Environment.Variable(ast.getName(),ast.getName(),scope.lookupVariable(ast.getName()).getType(),scope.lookupVariable(ast.getName()).getMutable(),obj);
            ast.setVariable(var);

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        //checks that provided arguments are assignable to parameter types
        List<Environment.Type> paramTypeList = scope.lookupFunction(ast.getName(),ast.getArguments().size()).getParameterTypes();
        List<Ast.Expression> providedParams = ast.getArguments();

        if (paramTypeList.size()!=providedParams.size()){
            throw new RuntimeException("wrong number of parameters");
        }

        for (int i = 0; i < paramTypeList.size(); i++){
            requireAssignable(paramTypeList.get(i),providedParams.get(i).getType());
        }

        ast.setFunction(scope.lookupFunction(ast.getName(),ast.getArguments().size()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        Environment.Type listType = ast.getType();
        //List<Ast.Expression> listVals = ast.getValues();

        for (Ast.Expression expr : ast.getValues()) {
            visit( expr );
            requireAssignable(listType,expr.getType());
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(type)){
        }else if(target.equals(Environment.Type.ANY)){
        }else if(target.equals(Environment.Type.COMPARABLE) &&
                (type.equals(Environment.Type.INTEGER) || (type.equals(Token.Type.DECIMAL) ||
                        (type.equals(Token.Type.CHARACTER))||(type.equals(Token.Type.STRING))))){
        }else{
           throw new RuntimeException("target type does not match the type being used or assigned");
        }
    }

}
