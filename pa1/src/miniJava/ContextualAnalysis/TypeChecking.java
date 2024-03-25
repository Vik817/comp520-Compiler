package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.TokenType;

import java.lang.reflect.Type;

public class TypeChecking implements Visitor<Object, TypeDenoter> {

    Package p;
    ErrorReporter reporter;

    public TypeChecking(Package tree, ErrorReporter er) {
        p = tree;
        reporter = er;
    }

    public void startTypeChecking() {
        p.visit(this, null);
    }

    @Override
    public TypeDenoter visitPackage(Package prog, Object arg) {
        for(ClassDecl cD : prog.classDeclList) {
            cD.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        for(FieldDecl fD : cd.fieldDeclList) {
            fD.visit(this, null);
        }
        for(MethodDecl mD : cd.methodDeclList) {
            mD.visit(this, null);
        }
        return cd.type;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        //Need of check if the fd type is not VOID, otherwise it is a methodDecl and we have a problem
        if(fd.type.typeKind == TypeKind.VOID) {
            reporter.reportError("Declaring as a FieldDecl but should be a MethodDecl");
        }
        return fd.type;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        for(ParameterDecl pd: md.parameterDeclList) {
            pd.visit(this, md);
        }
        for(Statement s : md.statementList) {
            s.visit(this, md);
        }
        return md.type;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        return pd.type;
    }

    //Types themselves don't need to return anything
    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        //System.out.println(decl.name);
        if(decl.name.equals("String")) {
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        //Might need to check the String issue
        return decl.type;
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        //type.eltType.visit(this, null);
        return null;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        for(Statement s: stmt.sl) {
            s.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        TypeDenoter a = (TypeDenoter)stmt.varDecl.visit(this, null);
        TypeDenoter b = (TypeDenoter)stmt.initExp.visit(this, null);
        //Need to check and validate that a = b
        if((typeComparator(a, b)) == null) {
            reportTypeError("Types are not equal");
        }
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {

        TypeDenoter currTypeDenoter = stmt.ref.referenceDeclaration.type;
        if(currTypeDenoter.typeKind == TypeKind.CLASS || currTypeDenoter instanceof BaseType) {
            TypeDenoter t = typeComparator((TypeDenoter)stmt.ref.visit(this, null), (TypeDenoter)stmt.val.visit(this, null));
            if(t == null) {
                reportTypeError("Assigning reference an invalid type");
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        if(stmt.ref.visit(this, null) instanceof ArrayType) {
            //check if the expression is an integer
            if (typeComparator(stmt.ix.visit(this, null), new BaseType(TypeKind.INT, null)) != null) {
                return null;
            } else {
                reportTypeError("Invalid expression type in IxAssigning");
            }
        } else {
            reportTypeError("Invalid expression type in IxAssigning");
        }
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) { //Checks if the arg list types match the provided method's parameter types
        if(!(stmt.methodRef.referenceDeclaration instanceof MethodDecl)) {
            reportTypeError("Not accessing a method");
            return null;
        }
        if(stmt.argList.size() == ((MethodDecl)stmt.methodRef.referenceDeclaration).parameterDeclList.size()) {
            for(int i = 0; i < stmt.argList.size(); i++) {
                TypeDenoter t = typeComparator(stmt.argList.get(i).visit(this, null),
                        ((MethodDecl)stmt.methodRef.referenceDeclaration).parameterDeclList.get(i).visit(this, null));
                if(t == null) {
                    reportTypeError("Passed argument type doesn't match parameter type");
                }
            }
        } else {
            reportTypeError("Provided argument number doesn't match Method's number of parameters");
            //throw new TypeCheckingError(); //Provided arguments don't match MethodDecl's number of parameters
        }
        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        //Need to check if method is void, if so nothing should be returned
        if(((MethodDecl)arg).type.typeKind == TypeKind.VOID) {
            if(stmt.returnExpr != null) {
                reportTypeError("Returning something to a void method");
            }
        } else { //If method is not void, need to check method's return type and see if it matches the Expression's type
            if(stmt.returnExpr != null) {
                TypeDenoter t = typeComparator(((MethodDecl)arg).type, stmt.returnExpr.visit(this, (MethodDecl)arg));
                if(t == null) {
                    reportTypeError("Incompatible return type");
                }
            } else {
                reportTypeError("Not returning anything to a method that needs a return value");
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        //If statements are always a comparison, so the stmt.Expression is the condition. Conditions are booleans
        TypeDenoter t = typeComparator(((TypeDenoter)stmt.cond.visit(this, null)), new BaseType(TypeKind.BOOLEAN, null));
        if(t == null) {
            reportTypeError("Not a condition/comparison in the if statement's condition");
        }
        stmt.thenStmt.visit(this, (MethodDecl)arg);
        if(stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        //Similar logic to the visitIfStmt
        TypeDenoter t = typeComparator(((TypeDenoter)stmt.cond.visit(this, null)), new BaseType(TypeKind.BOOLEAN, null));
        if(t == null) {
            reportTypeError("Not a condition/comparison in the if statement's condition");
        }
        stmt.body.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        TypeDenoter originalOperator = expr.expr.visit(this, (MethodDecl)arg);
        if(expr.operator.kind == TokenType.EXCLAMATION) { //Unary negation on a bool returns a bool
            TypeDenoter t = typeComparator(new BaseType(TypeKind.BOOLEAN, null), originalOperator);
            if(t == null) {
                reportTypeError("Boolean negation did not return type boolean");
            } else {
                return new BaseType(TypeKind.BOOLEAN, null);
            }
        } else if(expr.operator.kind == TokenType.NEGATIVE) { //Unary negation on an int returns an int
            TypeDenoter t = typeComparator(new BaseType(TypeKind.BOOLEAN, null), originalOperator);
            if(t == null) {
                reportTypeError("Integer negation did not return type integer");
            } else {
                return new BaseType(TypeKind.BOOLEAN, null);
            }
        } else {
            reportTypeError("Invalid operator for unary negation");
        }
        return new BaseType(TypeKind.ERROR, null);
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        TypeDenoter leftExpr = expr.left.visit(this, null);
        TypeDenoter rightExpr = expr.right.visit(this, null);
        Operator op = expr.operator;
        //Need to check every operation besides unary and see if it returned the right type
        //Could return the value of the operation
        if(op.spelling.equals("&&") || op.spelling.equals("||")) {
            TypeDenoter a = typeComparator(new BaseType(TypeKind.BOOLEAN, null), leftExpr);
            if(a == null) {
                reportTypeError("Left expression not a boolean in a boolean operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            TypeDenoter b = typeComparator(new BaseType(TypeKind.BOOLEAN, null), rightExpr);
            if(b == null) {
                reportTypeError("Right expression not a boolean in a boolean operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            //If neither is null, I can return a BOOLEAN
            return new BaseType(TypeKind.BOOLEAN, null);
        } else if(op.spelling.equals(">") || op.spelling.equals("<") ||
                op.spelling.equals(">=") || op.spelling.equals("<=")) {
            TypeDenoter a = typeComparator(new BaseType(TypeKind.INT, null), leftExpr);
            if(a == null) {
                reportTypeError("Left expression not an int in an int operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            TypeDenoter b = typeComparator(new BaseType(TypeKind.INT, null), rightExpr);
            if(b == null) {
                reportTypeError("Right expression not an int in an int operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            //If neither is null, I can return a BOOLEAN
            return new BaseType(TypeKind.BOOLEAN, null);
        } else if(op.spelling.equals("+") || op.spelling.equals("-") || op.spelling.equals("*") || op.spelling.equals("/")) {
            TypeDenoter a = typeComparator(new BaseType(TypeKind.INT, null), leftExpr);
            if(a == null) {
                reportTypeError("Left expression not an int in an int operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            TypeDenoter b = typeComparator(new BaseType(TypeKind.INT, null), rightExpr);
            if(b == null) {
                reportTypeError("Right expression not an int in an int operation");
                return new BaseType(TypeKind.ERROR, null);
            }
            //If neither is null, I can return a BOOLEAN
            return new BaseType(TypeKind.INT, null);
        } else if(op.spelling.equals("==") || op.spelling.equals("!=")) {
            TypeDenoter comparison = typeComparator(leftExpr, rightExpr);
            if(comparison == null) { //They aren't equal
                //First check if i can even compare the types
                return new BaseType(TypeKind.ERROR, null);
            } else {
                return new BaseType(comparison.typeKind, null);
//                if(op.spelling.equals("!=")) { //If they weren't supposed to be equal
//                    return new BaseType(TypeKind.BOOLEAN, null);
//                } else {
//                    reportTypeError("Expressions should be equal but aren't");
//                    return new BaseType(TypeKind.ERROR, null);
//                }
//                if(op.spelling.equals("==")) { //If they were supposed to be equal
//                    return new BaseType(TypeKind.BOOLEAN, null);
//                } else {
//                    reportTypeError("Expressions shouldn't be equal but are");
//                    return new BaseType(TypeKind.ERROR, null);
//                }
            }
        }
        return new BaseType(TypeKind.ERROR, null);
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        return expr.ref.visit(this, null);
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        //If i have something of the format Reference[Expression], The expression should be of type INT
        //And the Reference should be of type ARRAY
        if(expr.ref.visit(this, null) instanceof ArrayType) {
            //check if the expression is an integer
            if (typeComparator(expr.ixExpr.visit(this, null), new BaseType(TypeKind.INT, null)) != null) {
                return new ArrayType(expr.ixExpr.visit(this, null), null);
            } else {
                reportTypeError("Invalid expression type in IxExpression");
                return new BaseType(TypeKind.ERROR, null);
            }
        } else {
            return new BaseType(TypeKind.ERROR, null);
        }
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        //Similar to visitCallStmt
        //However, need to access the Method that the expression is referring to.
        //Can use the reference's declaration, which identifies where the reference's identifier was declared
        if(expr.argList.size() == ((MethodDecl)expr.functionRef.referenceDeclaration).parameterDeclList.size()) {
            for(int i = 0; i < expr.argList.size(); i++) {
                TypeDenoter t = typeComparator(expr.argList.get(i).visit(this, null),
                        ((MethodDecl)expr.functionRef.referenceDeclaration).parameterDeclList.get(i).visit(this, null));
                if(t == null) {
                    reportTypeError("Passed argument type doesn't match parameter type");
                }
            }
        } else {
            reportTypeError("Provided argument number doesn't match Method's number of parameters");
            return new BaseType(TypeKind.ERROR, null);
            //Provided arguments don't match MethodDecl's number of parameters
        }
        return ((MethodDecl)expr.functionRef.referenceDeclaration).type;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        //Could be IntLiteral or BooleanLiteral
        //Need to visit to see
        return expr.lit.visit(this, null);
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        if(expr.classtype.className.spelling.equals("String")) {
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        return expr.classtype;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {

        return null;
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        //Return "this" reference's type. The type of a this reference is the class it is in, which was given in Identification traversal
        return ref.referenceDeclaration.type;
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
//        if(ref.referenceDeclaration.type instanceof ClassType) {
//            if(((ClassType)ref.referenceDeclaration.type).className.spelling.equals("String")) {
//                return new BaseType(TypeKind.UNSUPPORTED, null);
//            }
//        }
        return ref.referenceDeclaration.type;
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        //Only thing that matters is the type of the leftmost Reference
        return ref.referenceDeclaration.type;
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        //Need to return the type of where the id was originally declared. Have to visit that
        return id.dec.visit(this, null);
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, null);
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, null);
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nullLit, Object arg) {
        return new BaseType(TypeKind.CLASS, null);
    }

    private TypeDenoter typeComparator(TypeDenoter a, TypeDenoter b) {
        //System.out.println(a.typeKind);
        //System.out.println(b.typeKind);
        if(a.typeKind == TypeKind.UNSUPPORTED && b.typeKind != TypeKind.ERROR) {
            return null;
        } else if (a.typeKind != TypeKind.ERROR && b.typeKind == TypeKind.UNSUPPORTED) {
            return null;
        }
        if(a.typeKind.equals(b.typeKind)) {
            if(a instanceof ClassType && b instanceof ClassType) {
                if(((ClassType) a).className.spelling.equals(((ClassType) b).className.spelling)) {
                    return a;
                } else {
                    return null;
                }
            }
            return a;
        } else {
            return null;
        }
    }

    private void reportTypeError(String message) {
        reporter.reportError(message);
    }
    class TypeCheckingError extends Error {
        private static final long serialVersionUID = -6461942006097999362L;


    }
}
