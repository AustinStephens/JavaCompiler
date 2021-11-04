package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;
    private Environment.Type currentMethodType;


    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(Ast.Field f : ast.getFields()) {
            visit(f);
        }

        for(Ast.Method m : ast.getMethods()) {
            visit(m);
        }

        Environment.Function main = scope.lookupFunction("main", 0);
        if(main.getReturnType().equals(Environment.Type.INTEGER)) throw new RuntimeException();
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type type = Environment.getType(ast.getTypeName());

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        Environment.Type returnType = Environment.Type.NIL;
        if(ast.getReturnTypeName().isPresent())
            returnType = Environment.getType(ast.getReturnTypeName().get());

        ArrayList<Environment.Type> paramTypes = new ArrayList<>();
        for(String t : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(t));
        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), paramTypes, returnType, args -> Environment.NIL));
        currentMethodType = returnType;

        scope = new Scope(scope);
        for(int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), paramTypes.get(i), Environment.NIL);
        }
        for(Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }
        /*for(Ast.Stmt stmt : ast.getStatements()) {
            if(stmt instanceof Ast.Stmt.Return) {
                Ast.Stmt.Return ret = (Ast.Stmt.Return) stmt;
                if(ret.getValue().getType().equals(returnType)) throw new RuntimeException("Return expression does not match function return type.");
            }
        }*/
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if(!(ast.getExpression() instanceof  Ast.Expr.Function)) throw new RuntimeException("Improper expression.");
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) throw new RuntimeException("Declaration must have type or value to infer type.");

        Environment.Type type = null;

        if(ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if(type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        if(!(ast.getReceiver() instanceof Ast.Expr.Access)) throw new RuntimeException("Not an assignable expression.");
        visit(ast.getReceiver());
        visit(ast.getValue());
        if(!(ast.getReceiver().getType().equals(ast.getValue().getType()))) throw new RuntimeException("Assignment type does not match.");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if(ast.getThenStatements().isEmpty()) throw new RuntimeException("Cannot have an empty then statement.");
        try {
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        try {
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if(ast.getStatements().isEmpty()) throw new RuntimeException("Cannot have an empty for loop");

        try {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            for(Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());
        requireAssignable(currentMethodType, ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if(ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if(ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if(ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        } else if(ast.getLiteral() instanceof BigInteger) {
            BigInteger bigInt = (BigInteger) ast.getLiteral();
            if(bigInt.bitLength() > 31) throw new RuntimeException("Cannot store an integer over 32 bits.");
            ast.setType(Environment.Type.INTEGER);
        } else if(ast.getLiteral() instanceof BigDecimal) {
            BigDecimal bigDec = (BigDecimal) ast.getLiteral();
            double dblVal = bigDec.doubleValue();
            if(dblVal == Double.NEGATIVE_INFINITY || dblVal == Double.POSITIVE_INFINITY) throw new RuntimeException("Cannot store a decimal of this size");
            ast.setType(Environment.Type.DECIMAL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        if(!(ast.getExpression() instanceof Ast.Expr.Binary)) throw new RuntimeException("Cannot have a singular expression within parenthesis");
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        if(ast.getOperator().equals("AND") || ast.getOperator().equals("OR")) {
            if(ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN))
                ast.setType(Environment.Type.BOOLEAN);
            else
                throw new RuntimeException("Must use AND/OR with Boolean values.");
        } else if(ast.getOperator().equals("+") || ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")) {
            if(ast.getOperator().equals("+") && (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)))
                ast.setType(Environment.Type.STRING);
            else if(ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                if(!ast.getRight().getType().equals(Environment.Type.INTEGER)) throw new RuntimeException("Types must match on each side of the operation.");
                ast.setType(Environment.Type.INTEGER);
            } else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if(!ast.getRight().getType().equals(Environment.Type.DECIMAL)) throw new RuntimeException("Types must match on each side of the operation.");
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid operation with given Types");
            }
        } else {
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        } else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        Environment.Function func = null;
        if(ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            func = ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size());
        } else {
            func = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        }
        for(int i = 0; i < ast.getArguments().size(); i++) {
            if(!(ast.getReceiver().isPresent() && i == 0)) {
                visit(ast.getArguments().get(i));
                requireAssignable(func.getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
        }
        ast.setFunction(func);
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(type)) return;

        if(target.equals(Environment.Type.ANY)) return;

        if(target.equals(Environment.Type.COMPARABLE) &&
                (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)))
            return;

        throw new RuntimeException("Data types do not match.");
    }

}
