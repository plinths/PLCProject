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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
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
        throw new UnsupportedOperationException();  // TODO
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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
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
        throw new UnsupportedOperationException();//TODO
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
