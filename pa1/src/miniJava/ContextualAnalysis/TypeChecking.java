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
        return null;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        //Need of check if the fd type is not VOID, otherwise it is a methodDecl and we have a problem
        if(fd.type.typeKind == TypeKind.VOID) {
            reporter.reportError("Declaring as a FieldDecl but should be a MethodDecl");
        }
        return null;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        return null;
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
        return null;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
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
        return null;
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nullLit, Object arg) {
        return null;
    }

    private TypeDenoter typeComparator(TypeDenoter a, TypeDenoter b) {
        if(a.typeKind.equals(b.typeKind)) {
            return a;
        }
        return null;
    }
}
