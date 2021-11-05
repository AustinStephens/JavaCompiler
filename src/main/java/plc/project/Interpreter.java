package plc.project;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);
    private HashMap<String, Scope> methodScopes = new HashMap<>();

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

        for(Ast.Field field : ast.getFields()) {
            visit(field);
        }

        for(Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        Environment.Function main = scope.lookupFunction("main", 0);
        return main.invoke(Collections.emptyList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {

        if(ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        String methodName = ast.getName() + "/" + ast.getParameters().size();
        Scope newScope = new Scope(scope);
        methodScopes.put(methodName, newScope);

        scope.defineFunction(ast.getName(),
                ast.getParameters().size(),
                args -> {
                    Scope prevScope = scope;
                    scope = new Scope(methodScopes.get(ast.getName() + "/" + ast.getParameters().size()));
                    List<String> params = ast.getParameters();
                    for(int i = 0; i < params.size(); i++) {
                        scope.defineVariable(params.get(i), args.get(i));
                    }

                    List<Ast.Stmt> stmts = ast.getStatements();
                    try {
                        for(Ast.Stmt stmt : stmts) {
                            visit(stmt);
                        }
                        scope = prevScope;
                    } catch(Return e) {
                        scope = prevScope;
                        return e.value;
                    }
                    return Environment.NIL;
                });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {

        if(ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if(ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access rec = (Ast.Expr.Access) ast.getReceiver();
            Environment.PlcObject val = visit(ast.getValue());

            if(rec.getReceiver().isPresent()) {
                String name = rec.getName();
                visit(rec.getReceiver().get()).setField(name, val);
            } else {
                scope.lookupVariable(rec.getName()).setValue(val);
            }
        } else {
            throw new RuntimeException("Cannot assign this to a value");
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {

        Environment.PlcObject o = visit(ast.getCondition());
        if(requireType(Boolean.class, o)) {
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable<Environment.PlcObject> it = requireType(Iterable.class, visit(ast.getValue()));
        String name = ast.getName();

        Iterator<Environment.PlcObject> iter = it.iterator();

        iter.forEachRemaining(args -> {
                scope = new Scope(scope);
                scope.defineVariable(name, iter.next());
                List<Ast.Stmt> stmts = ast.getStatements();
                for(Ast.Stmt stmt : stmts) {
                    visit(stmt);
                }
                scope = scope.getParent();
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {

        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        Environment.PlcObject obj = visit(ast.getValue());
        throw new Return(obj);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null) return Environment.NIL;
        else return new Environment.PlcObject(scope, ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        Environment.PlcObject obj1 = visit(ast.getLeft());

        if(obj1.getValue() instanceof Boolean) {
            boolean left = requireType(Boolean.class, obj1);
            if(op.equals("AND") && !left) return Environment.create(Boolean.FALSE);
            else if(op.equals("OR") && left) return Environment.create(Boolean.TRUE);
        }

        Environment.PlcObject obj2 = visit(ast.getRight());

        if(op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
            Comparable left = requireType(Comparable.class, obj1);
            Comparable right = requireType(Comparable.class, obj2);
            requireType(obj1.getValue().getClass(), obj2);
            int comp = left.compareTo(right);
            if(comp == 0) {
                if(op.equals("<=") || op.equals(">=")) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            } else if(comp > 0) {
                if(op.equals(">") || op.equals(">=")) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            } else {
                if(op.equals("<") || op.equals("<=")) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            }
        } else if(op.equals("==")) {
            return Environment.create(obj1.getValue().equals(obj2.getValue()));
        } else if(op.equals("!=")) {
            return Environment.create(!obj1.getValue().equals(obj2.getValue()));
        } else if(obj1.getValue() instanceof String || obj2.getValue() instanceof String) {
            if(op.equals("+")) return Environment.create(obj1.getValue().toString().concat(obj2.getValue().toString()));
        } else if(obj1.getValue() instanceof BigInteger) {
                BigInteger int1 = (BigInteger) obj1.getValue();
                BigInteger int2 = requireType(BigInteger.class, obj2);

                if(op.equals("+"))
                    return Environment.create(int1.add(int2));
                else if(op.equals("-"))
                    return Environment.create(int1.subtract(int2));
                else if(op.equals("*"))
                    return Environment.create(int1.multiply(int2));
                else if(op.equals("/")) {
                    if(int2.equals(BigInteger.ZERO)) throw new RuntimeException("Divide by 0 error.");
                    return Environment.create(int1.divide(int2));
                }
        } else if(obj1.getValue() instanceof BigDecimal) {
                BigDecimal dec1 = (BigDecimal) obj1.getValue();
                BigDecimal dec2 = requireType(BigDecimal.class, obj2);

                if(op.equals("+"))
                    return Environment.create(dec1.add(dec2));
                else if(op.equals("-"))
                    return Environment.create(dec1.subtract(dec2));
                else if(op.equals("*"))
                    return Environment.create(dec1.multiply(dec2));
                else if(op.equals("/")) {
                    if(dec2.equals(BigDecimal.ZERO)) throw new RuntimeException("Divide by 0 error.");
                    return Environment.create(dec1.divide(dec2, RoundingMode.HALF_EVEN));
                }
        } else if(obj1.getValue() instanceof Boolean) {
            boolean left = requireType(Boolean.class, visit(ast.getLeft()));
            boolean right = requireType(Boolean.class, visit(ast.getRight()));

            if(op.equals("AND")) return Environment.create(left && right);
            else if(op.equals("OR")) return Environment.create(left || right);
        }

        throw new RuntimeException("Error");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        Environment.PlcObject obj = null;
        if(ast.getReceiver().isPresent()) obj = visit(ast.getReceiver().get());

        if(obj != null) {
            return obj.getField(ast.getName()).getValue();
        } else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        Environment.PlcObject obj = null;
        if(ast.getReceiver().isPresent()) obj = visit(ast.getReceiver().get());

        ArrayList<Environment.PlcObject> args = new ArrayList<Environment.PlcObject>();
        for(Ast.Expr arg : ast.getArguments()) {
            args.add(visit(arg));
        }

        if(obj != null) {
            return obj.callMethod(ast.getName(), args);
        } else {
            return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
        }
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
