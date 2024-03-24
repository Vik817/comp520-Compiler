package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

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
        for(MethodDecl fD : cd.methodDeclList) {
            cd.visit(this, null);
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
            pd.visit(this, null);
        }
        for(Statement s : md.statementList) {
            s.visit(this, null);
        }
        return md.type;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.visit(this, null);
        return pd.type;
    }

    //Types themselves don't need to return anything
    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
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
        type.eltType.visit(this, null);
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
        if(!(equalTypes(a, b))) {
            reporter.reportError("Types are not equal");
        }
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter currTypeDenoter = stmt.ref.referenceDeclaration.type;
        if(currTypeDenoter.typeKind == TypeKind.CLASS || currTypeDenoter instanceof BaseType) {
            typeComparator((TypeDenoter)stmt.ref.visit(this, null), (TypeDenoter)stmt.val.visit(this, null));
        }
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        return null;
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
        if(a.typeKind.equals(b.typeKind)) {
            return a;
        } else {
            throw new TypeCheckingError();
        }
    }

    private boolean equalTypes(TypeDenoter a, TypeDenoter b) {
        if(a.typeKind.equals(b.typeKind)) {
            return true;
        }
        return false;
    }

    class TypeCheckingError extends Error {
        private static final long serialVersionUID = -6461942006097999362L;


    }
}
