package plc.project;

import java.io.PrintWriter;
import java.util.List;

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
        // create class start
        print("public class Main {");
        newline(0);
        ++indent;
        // declare fields
        for(int i = 0; i < ast.getFields().size(); i++) {
            newline(indent);
            print(ast.getFields().get(i));
        }
        if(ast.getFields().size() > 0) newline(0);
        // declare main

        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent+1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");
        // declare methods
        for(int i = 0; i < ast.getMethods().size(); i++) {
            newline(0);
            newline(indent);
            print(ast.getMethods().get(i));
        }
        newline(0);
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //function signature
        print(ast.getFunction().getReturnType().getJvmName(),
                " ", ast.getName(), "(");
        //parameter list
        for(int i  = 0; i < ast.getParameters().size(); i++) {
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName(),
                    " ", ast.getParameters().get(i));
            if(i != ast.getParameters().size()-1) print(", ");
        }
        print(") {");
        //statement list
        if(!ast.getStatements().isEmpty()) {
            printStatements(ast.getStatements());
        }
        //end function
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(), ") {");
        printStatements(ast.getThenStatements());

        if(ast.getElseStatements().size() > 0) {
            print("} else {");
            printStatements(ast.getElseStatements());

        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int ", ast.getName(), " : ", ast.getValue(), ") {");
        printStatements(ast.getStatements());
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        if(!ast.getStatements().isEmpty()) {
            printStatements(ast.getStatements());
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if(ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        } else if(ast.getType().equals(Environment.Type.CHARACTER)) {
            print("\'", ast.getLiteral(), "\'");
        } else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String jvmOp;
        if(ast.getOperator().equals("AND")) jvmOp = "&&";
        else if(ast.getOperator().equals("OR")) jvmOp = "||";
        else jvmOp = ast.getOperator();

        print(ast.getLeft(), " ", jvmOp, " ", ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent())
            print(ast.getReceiver().get(), ".");
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if(ast.getReceiver().isPresent())
            print(ast.getReceiver().get(), ".");
        print(ast.getFunction().getJvmName(), "(");
        for(int i = 0; i < ast.getArguments().size(); i++) {
            print(ast.getArguments().get(i));
            if(i != ast.getArguments().size()-1) print(", ");
        }
        print(")");
        return null;
    }

    private Void printStatements(List<Ast.Stmt> stmts) {
        indent++;
        for(int i = 0; i < stmts.size(); i++) {
            newline(indent);
            print(stmts.get(i));
        }
        newline(--indent);

        return null;
    }

}
