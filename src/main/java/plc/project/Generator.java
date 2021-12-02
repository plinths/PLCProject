package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {


        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getValue().get() instanceof Ast.Expression.PlcList){
            print(((Ast.Expression.PlcList) ast.getValue().get()).getValues().get(0).getType().getJvmName(),"[] ",ast.getName()," = ");
            print(ast.getValue().get());
            print(";");
        }
        else if (ast.getMutable()){//mutable
            print(ast.getTypeName()," ",ast.getName());
            if (ast.getValue().isPresent()){
                print(" = ",ast.getValue().get(),";");
            }
        }else {//immutable
            print("final ",ast.getTypeName()," ",ast.getName());
            if (ast.getValue().isPresent()){
                print(" = ",ast.getValue().get(),";");
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getReturnTypeName().get()," ",ast.getName(),"(");
        for (int i = 0 ; i < ast.getParameters().size(); i++){//parameters
            if (i == 0){
                print(ast.getParameterTypeNames().get(i)," ",ast.getParameters().get(i));
            } else{
                print(" ",ast.getParameterTypeNames().get(i)," ",ast.getParameters().get(i));
            }

            if (i != ast.getParameters().size()-1){
                print(",");
            }
        }
        print(") {");//closing parenthesis + opening brackets

        if (ast.getStatements().isEmpty()){
            print("}");
        }else{
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size();i++){
                if (i!=0){
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            indent--;
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(),";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(),";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
       print("if (", ast.getCondition(),") {");

       if (!ast.getThenStatements().isEmpty()) {//then statements
           newline(++indent);//handles i == 0 case
           //handle statements in the if
           for (int i = 0; i < ast.getThenStatements().size(); i++) {
               if (i != 0) {
                   newline(indent);
               }
               print(ast.getThenStatements().get(i));//print next statement
           }
           newline(--indent);
       }
       print("}");

       if (!ast.getElseStatements().isEmpty()){//else statements
           print(" else {");
           newline(++indent);//handles i == 0 case
           //handle statements in the if
           for (int i = 0; i < ast.getElseStatements().size(); i++) {
               if (i != 0) {
                   newline(indent);
               }
               print(ast.getElseStatements().get(i));//print next statement
           }
           newline(--indent);
           print("}");
       }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (",ast.getCondition(),") {");
        ++indent;
        for (int i =0; i < ast.getCases().size(); i++){
            newline(indent);
            print(ast.getCases().get(i));
        }
        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (!ast.getValue().isPresent()){//handles default format
            print("default:");
        }else print("case ", ast.getValue().get(), ":");

        if(!ast.getStatements().isEmpty()){
            //setup next line
            newline(++indent);//handles i == 0 case
            //handle statements
            for (int i = 0; i < ast.getStatements().size(); i++){
                print(ast.getStatements().get(i));//print next statement
                if (i!=ast.getStatements().size()-1){
                    newline(indent);
                }
            }
            --indent;//setup next line
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        if(!ast.getStatements().isEmpty()){
            //setup next line
            newline(++indent);//handles i == 0 case
            //handle statements in the while
            for (int i = 0; i < ast.getStatements().size(); i++){
                if(i != 0){
                   newline(indent);
                }
                print(ast.getStatements().get(i));//print next statement
            }
            newline(--indent);//setup next line
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
       print("return ");
       print(ast.getValue());
       print(";");

       return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType().equals(Environment.Type.STRING)){
            print("\"",ast.getLiteral(),"\"");
        } else if (ast.getType().equals(Environment.Type.CHARACTER)){
            print("'",ast.getLiteral(),"'");
        } else print(ast.getLiteral());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")){
            print("Math.pow(",ast.getLeft(),", ",ast.getRight(),")");
        }else {
            print(ast.getLeft());
            print(" ", ast.getOperator(), " ");
            print(ast.getRight());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        for(int i = 0; i < ast.getArguments().size(); i++){
            print(ast.getArguments().get(i));
            if (i!=ast.getArguments().size()-1){
                print(",");
            }
        }
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");

        for (int i = 0 ; i < ast.getValues().size(); i++){
            if (i == 0){
                print(ast.getValues().get(i));
            }
            else{
                print(" ",ast.getValues().get(i));
            }

            if (i != ast.getValues().size()-1){
                print(",");
            }
        }

        print("}");

        return null;
    }

}
