//Andres Portillo

package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Generates Java code from an abstract syntax tree (AST).
 * Implements the visitor pattern to traverse and convert AST nodes.
 */
public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer; // Writer to output the generated code
    private int indentLevel = 0; // Tracks current indentation level

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Helper method to print objects to the writer.
     * Supports visiting AST nodes or writing plain strings.
     */
    private void print(Object... objs) {
        for (Object obj : objs) {
            if (obj instanceof Ast) {
                visit((Ast) obj); // Visit AST node
            } else {
                writer.write(obj.toString()); // Write plain text
            }
        }
    }

    /**
     * Helper method to apply indentation based on the current indent level.
     */
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            writer.write("    "); // 4 spaces per indent level
        }
    }

    /**
     * Adds a newline to the output.
     */
    private void newline() {
        writer.write(System.lineSeparator());
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Start the main class
        print("public class Main {");
        newline();
        newline();

        indentLevel++;

        // Visit and generate code for fields
        for (Ast.Field field : ast.getFields()) {
            indent();
            visit(field);
            newline();
        }

        if (!ast.getFields().isEmpty()) {
            newline(); // Add blank line after fields if any exist
        }

        // Generate main method
        indent();
        print("public static void main(String[] args) {");
        newline();
        indentLevel++;
        indent();
        print("System.exit(new Main().main());"); // Call main instance method
        newline();
        indentLevel--;
        indent();
        print("}");
        newline();

        // Generate methods
        if (!ast.getMethods().isEmpty()) {
            newline(); // Add blank line before methods
            for (int i = 0; i < ast.getMethods().size(); i++) {
                if (i > 0) {
                    newline(); // Add blank line between methods
                }
                visit(ast.getMethods().get(i));
            }
        }

        indentLevel--;
        indent();
        print("}"); // End of the main class
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        // Generate code for a field declaration
        indent();
        String type = ast.getVariable().getType().getJvmName(); // Field type
        String name = ast.getVariable().getJvmName(); // Field name
        print(type, " ", name);
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get()); // Visit and generate value
        }
        print(";");
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // Generate method declaration
        indent();
        String returnType = ast.getFunction().getReturnType().getJvmName(); // Return type
        String methodName = ast.getFunction().getJvmName(); // Method name
        print(returnType, " ", methodName, "(");

        // Generate parameters
        List<String> parameters = ast.getParameters();
        List<Environment.Type> parameterTypes = ast.getFunction().getParameterTypes();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(parameterTypes.get(i).getJvmName(), " ", parameters.get(i));
        }
        print(") {");
        newline();
        indentLevel++;

        // Generate method body statements
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }

        indentLevel--;
        indent();
        print("}"); // Close method
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        // Generate code for an expression statement
        indent();
        visit(ast.getExpression());
        print(";");
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        // Generate variable declaration
        indent();
        String type = ast.getVariable().getType().getJvmName();
        String name = ast.getVariable().getJvmName();
        print(type, " ", name);
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        // Generate assignment statement
        indent();
        visit(ast.getReceiver()); // Visit left-hand side
        print(" = ");
        visit(ast.getValue()); // Visit right-hand side
        print(";");
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        // Generate if statement
        indent();
        print("if (");
        visit(ast.getCondition()); // Condition expression
        print(")");
        if (!ast.getThenStatements().isEmpty()) {
            print(" {");
            newline();
            indentLevel++;
            for (Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
            indentLevel--;
            indent();
            print("}");
        } else {
            print(" {}");
        }
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline();
            indentLevel++;
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
            indentLevel--;
            indent();
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        // Generate for loop
        indent();
        String type = "var";
        String name = ast.getName();
        print("for (", type, " ", name, " : ");
        visit(ast.getValue());
        print(")");
        if (!ast.getStatements().isEmpty()) {
            print(" {");
            newline();
            indentLevel++;
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
            indentLevel--;
            indent();
            print("}");
        } else {
            print(" {}");
        }
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        // Generate while loop
        indent();
        print("while (");
        visit(ast.getCondition()); // Condition
        print(")");
        if (!ast.getStatements().isEmpty()) {
            print(" {");
            newline();
            indentLevel++;
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
            indentLevel--;
            indent();
            print("}");
        } else {
            print(" {}");
        }
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        // Generate return statement
        indent();
        print("return ");
        visit(ast.getValue());
        print(";");
        newline();
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        // Generate literal value
        Object value = ast.getLiteral();
        if (value instanceof String) {
            print("\"", ((String) value).replace("\"", "\\\""), "\"");
        } else if (value instanceof Character) {
            print("'", ((Character) value).toString().replace("'", "\\'"), "'");
        } else if (value instanceof BigInteger || value instanceof BigDecimal || value instanceof Boolean) {
            print(value.toString());
        } else if (value == null) {
            print("null");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        // Generate grouped expression
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        // Generate binary expression
        visit(ast.getLeft());
        print(" ", getOperator(ast.getOperator()), " ");
        visit(ast.getRight());
        return null;
    }

    private String getOperator(String operator) {
        // Translate high-level operators to Java operators
        switch (operator) {
            case "AND": return "&&";
            case "OR": return "||";
            case "<": case ">": case "<=": case ">=": case "==": case "!=":
                return operator;
            case "ADD": case "+": return "+";
            case "SUBTRACT": case "-": return "-";
            case "MULTIPLY": case "*": return "*";
            case "DIVIDE": case "/": return "/";
            case "EXPONENT": return "^";
            default: return operator;
        }
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        // Generate access expression (e.g., variable or field)
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        // Generate function call
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        String functionName = ast.getFunction().getJvmName();
        print(functionName, "(");
        List<Ast.Expr> arguments = ast.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                print(", ");
            }
            visit(arguments.get(i));
        }
        print(")");
        return null;
    }
}
