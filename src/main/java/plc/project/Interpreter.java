package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for (int i = 0; i < ast.getGlobals().size(); i++){

            visit(ast.getGlobals().get(i));

        }

        for (int i = 0; i < ast.getFunctions().size(); i++){

            visit(ast.getFunctions().get(i));

        }

        Environment.Function bigMain = scope.lookupFunction("main",0);
        List<Environment.PlcObject> list = new ArrayList<>();


        return bigMain.invoke(list);
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        if ( ast.getMutable() && (ast.getValue().isPresent())) {//is mutable and value Present
            scope.defineVariable(ast.getName(),Boolean.TRUE,visit(ast.getValue().get()));
        }else if (ast.getMutable()){//is mutable no value Present
            scope.defineVariable(ast.getName(),Boolean.TRUE,Environment.NIL);
        } else{
            scope.defineVariable(ast.getName(),Boolean.FALSE, visit(ast.getValue().get()));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        scope = new Scope(scope);//new scope remember to restore when finished

        scope.defineFunction(ast.getName(),ast.getParameters().size(),args -> {
            List<String> params = ast.getParameters();

            for (int i = 0; i < params.size(); i++) {
                scope.defineVariable(params.get(i), true, null);
            }

            List<Ast.Statement> stmnts = ast.getStatements();

            for (int i = 0; i < stmnts.size(); i++) {
                try {
                    visit(stmnts.get(i));
                }catch(Interpreter.Return e){
                    scope = scope.getParent();
                    return e.value;
                }
            }
            scope = scope.getParent();
            return Environment.NIL;
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if ( ast.getValue().isPresent() ){
            scope.defineVariable( ast.getName(),
                    true,
                    visit( ast.getValue().get()));
        } else {
            scope.defineVariable( ast.getName(),
                    true,
                    Environment.NIL );
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if (!(ast.getReceiver() instanceof Ast.Expression.Access)){//ensure reciever is ...
            throw new RuntimeException("variable type is not assignable");//TODO: ANY OTHER OPTIONS?
        }

        String varName = ((Ast.Expression.Access) ast.getReceiver()).getName();
        //search for variable
        Environment.Variable var = scope.lookupVariable(varName);

        if (!(var.getMutable())) {//if immutable
            throw new UnsupportedOperationException("attempt to assign immutable variable");
        }




        //set variable in current scope

        //I wrote this and i still don't understand how it works but it passes the test
        if (((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()) {//if plc list

            BigInteger offset = requireType(BigInteger.class, visit( ((Ast.Expression.Access) ast.getReceiver()).getOffset().get() ) );

            Object tempList = var.getValue().getValue();
            List tempTempList = (List)tempList;

            int i=0;
            for (int j =0;j<offset.intValue();j++)
            {
                i++;
            }

            //use offset variable to return appropriate list value
            tempTempList.set(i,visit(ast.getValue()).getValue());
            Environment.PlcObject newList = Environment.create(tempTempList);
            var.setValue(newList);

        }else{
            var.setValue(visit(ast.getValue()));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        if( requireType(Boolean.class, visit(ast.getCondition()) )){

            scope = new Scope(scope);

            ast.getThenStatements().forEach(this::visit);

        }else{

            scope = new Scope(scope);

            ast.getElseStatements().forEach(this::visit);

        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        scope = new Scope(scope);

        int caseNum = ast.getCases().size();

        for (int i = 0; i < caseNum; i++){//check if condition is equivalent to case
            if (ast.getCondition().equals(ast.getCases().get(i))){

                visit(ast.getCases().get(i));

                break;
            }
            if (i==(caseNum-1)){//default case

                visit(ast.getCases().get(i));

            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        int numStatements = ast.getStatements().size();

        for(int i = 0; i < numStatements; i++){

            visit(ast.getStatements().get(i));

        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while ( requireType( Boolean.class, visit( ast.getCondition()))) {
            try{
                scope = new Scope(scope);

                ast.getStatements().forEach(this::visit);

            }finally{
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        throw new Return(visit(ast.getValue()));

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        Environment.PlcObject result;

        if (ast.getLiteral()==null){
            result = Environment.NIL;
        }else result = Environment.create(ast.getLiteral());

        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {

        return Environment.create(visit(ast.getExpression()).getValue());

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {

        Environment.PlcObject result = Environment.NIL;

        switch(ast.getOperator()){
            case "&&":

                if ( requireType (Boolean.class, visit(ast.getLeft() ) ) == Boolean.FALSE){//LHS nogo return false

                    result = Environment.create(Boolean.FALSE);

                }else if ( requireType (Boolean.class, visit(ast.getRight() ) ) == Boolean.TRUE){//both good return true

                    result = Environment.create(Boolean.TRUE);

                }else {

                    result = Environment.create(Boolean.FALSE);

                }
                break;
            case "||":

                if ( requireType (Boolean.class, visit(ast.getLeft() ) ) == Boolean.TRUE){//LHS nogo return false

                    result = Environment.create(Boolean.TRUE);

                }else if ( requireType (Boolean.class, visit(ast.getRight() ) ) == Boolean.TRUE){//both good return true

                    result = Environment.create(Boolean.TRUE);

                }else {

                    result = Environment.create(Boolean.FALSE);

                }
                break;
            case "<":
                if (visit(ast.getLeft()).getClass() != ( visit(ast.getRight()).getClass())){//not of same type
                    throw new RuntimeException("Left hand side and right hand side of binary expression are of different types.");
                }

                Comparable LHS = requireType(Comparable.class, visit(ast.getLeft()));
                Comparable RHS = requireType(Comparable.class, visit(ast.getRight()));
                int compareResult = LHS.compareTo(RHS);

                if (compareResult >= 0){
                    result = Environment.create(Boolean.FALSE);
                }else result = Environment.create(Boolean.TRUE);
                break;

            case ">":
                if (visit(ast.getLeft()).getClass() != visit(ast.getRight()).getClass()){//not of same type
                    throw new RuntimeException("Left hand side and right hand side of binary expression are of different types.");
                }

                Comparable LHS1 = requireType(Comparable.class, visit(ast.getLeft()));
                Comparable RHS1 = requireType(Comparable.class, visit(ast.getRight()));
                int compareResult1 = LHS1.compareTo(RHS1);

                if (compareResult1 <= 0){
                    result = Environment.create(Boolean.FALSE);
                }else result = Environment.create(Boolean.TRUE);
                break;
            case "==":
                boolean Res;
                Res = visit(ast.getLeft()).equals(visit(ast.getRight()));

                result = Res ? Environment.create(Boolean.TRUE) : Environment.create(Boolean.FALSE);

                break;
            case "!=":
                boolean Res1;
                Res1 = visit(ast.getLeft()).equals(visit(ast.getRight()));

                result = Res1 ? Environment.create(Boolean.FALSE) : Environment.create(Boolean.TRUE);

                break;
            case "+":

                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());

                if (lhs.getValue() instanceof java.lang.String || rhs.getValue() instanceof java.lang.String){//concatenation

                    result = Environment.create( lhs.getValue().toString() + rhs.getValue().toString() );

                }else if ( (lhs.getValue() instanceof java.math.BigInteger) && (rhs.getValue() instanceof java.math.BigInteger)){

                    result = Environment.create( ((BigInteger) lhs.getValue()).add((BigInteger) rhs.getValue()) );

                }else if ( (lhs.getValue() instanceof java.math.BigDecimal) && (rhs.getValue() instanceof java.math.BigDecimal)){

                    result = Environment.create( ((BigDecimal) lhs.getValue()).add((BigDecimal) rhs.getValue()) );

                }else throw new RuntimeException("rhs and lhs of different types");

                break;
            case "-":

                Environment.PlcObject lhs0 = visit(ast.getLeft());
                Environment.PlcObject rhs0 = visit(ast.getRight());

                if (lhs0.getValue() instanceof java.math.BigInteger && rhs0.getValue() instanceof java.math.BigInteger){

                    result = Environment.create( ((BigInteger) lhs0.getValue()).subtract((BigInteger) rhs0.getValue()) );

                }else if (lhs0.getValue() instanceof java.math.BigDecimal && rhs0.getValue() instanceof java.math.BigDecimal){

                    result = Environment.create( ((BigDecimal) lhs0.getValue()).subtract((BigDecimal) rhs0.getValue()) );

                }else throw new RuntimeException("LHS and RHS expressions ");

                break;
            case "*":

                Environment.PlcObject lhs1 = visit(ast.getLeft());
                Environment.PlcObject rhs1 = visit(ast.getRight());

                if (lhs1.getValue() instanceof java.math.BigInteger && rhs1.getValue() instanceof java.math.BigInteger){

                    result = Environment.create( ((BigInteger) lhs1.getValue()).multiply((BigInteger) rhs1.getValue()) );

                }else if (lhs1.getValue() instanceof java.math.BigDecimal && rhs1.getValue() instanceof java.math.BigDecimal){

                    result = Environment.create( ((BigDecimal) lhs1.getValue()).multiply((BigDecimal) rhs1.getValue()) );

                }else throw new RuntimeException("LHS and RHS expressions ");

                break;
            case "/":

                Environment.PlcObject divLHS = visit(ast.getLeft());
                Environment.PlcObject divRHS = visit(ast.getRight());

                if(divLHS.getValue() instanceof java.math.BigInteger && divRHS.getValue() instanceof java.math.BigInteger){//both big integer

                    result = Environment.create(( (BigInteger) divLHS.getValue() ).divide( (BigInteger) divRHS.getValue() ) );

                }else if(divLHS.getValue() instanceof java.math.BigDecimal && divRHS.getValue() instanceof java.math.BigDecimal){//both big decimal

                    result = Environment.create( ((BigDecimal) divLHS.getValue()).divide((BigDecimal) divRHS.getValue(), RoundingMode.HALF_EVEN));

                }else if(divRHS.getValue().equals(0)){//denominator is zero

                    throw new RuntimeException("denominator cannot be 0");

                }else throw new RuntimeException("lhs and rhs must be of same type");



                break;
            case "^":

                Environment.PlcObject base = visit(ast.getLeft());
                Environment.PlcObject exp = visit(ast.getRight());

                if (!(base.getValue() instanceof java.math.BigInteger)){
                    throw new RuntimeException("Exponent must be of type Big Integer");
                }

                //TODO:do math

                break;

        }
        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject result;

        if ( ast.getOffset().isPresent() ){//if object contains optional access expression
            BigInteger offset = requireType(BigInteger.class, visit( ast.getOffset().get() ) );
            Environment.Variable temp = scope.lookupVariable(ast.getName());
            Object tempList = temp.getValue().getValue();
            List tempTempList = (List)tempList;
            //List temtem = visit(temp.getValue().getValue());
            int i=0;


            for (int j =0;j<offset.intValue();j++)
            {
                i++;
            }

            //use offset variable to return appropriate list value
            result = Environment.create(tempTempList.get(i));//TODO

        }else{//return variable

            String val = scope.lookupVariable(ast.getName()).getValue().getValue().toString();

            result = Environment.create(val);

        }

        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {//TODO
        int argLength = ast.getArguments().size();
        Environment.PlcObject result;

        if (argLength == 0){//no arguments
            result = Environment.create(ast.getName());
        }else {//arguments
            List<Environment.PlcObject> args = new ArrayList<>();

            for (int i = 0; i < argLength; i++) {
                args.add( visit(ast.getArguments().get(i)) );
            }
            //lookup
            Environment.Function foo = getScope().lookupFunction(ast.getName(),argLength);

            //invoke function
            result = foo.invoke(args);
        }
        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        int length = ast.getValues().size();
        List resultList = new ArrayList<Object>();

        for (int i = 0; i < length; i++){
            resultList.add(visit(ast.getValues().get(i)).getValue());
        }

        return Environment.create(resultList);

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}