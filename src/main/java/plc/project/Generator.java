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
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
//TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //TODO
        return null;
    }

}
