package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.*;
import miniJava.ErrorReporter;

public class Identification implements Visitor {

    private ScopedIdentification si;
    private ErrorReporter er;
    Package pack;

    public Identification(Package tree, ErrorReporter r) {
        this.er = r;
        this.si = new ScopedIdentification(); //Maybe have to pass in the errorReporter
        pack = tree;
    }

    public void startIdentifying() {
        pack.visit(this, null);
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    ///////////////////////////////////////////////////////////////////////////////
    @Override
    public Object visitPackage(Package prog, Object arg) { //Want to start opening scope here
        si.openScope();
        for(ClassDecl cD: prog.classDeclList) { //Add all classDecls in the Package's AST to level 0 IDTable
            si.addDeclaration(cD.name, cD, null);
        }
        for(ClassDecl cD: prog.classDeclList) {
            cD.visit(this, null);
        }
        si.closeScope();
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        si.openScope();
        //Add all MemberDecls (field and method decls) in the Package's AST to level 1 IDTable
        for(FieldDecl fD: cd.fieldDeclList) {
            si.addDeclaration(fD.name, fD, cd);
        }
        for(MethodDecl mD: cd.methodDeclList) {
            si.addDeclaration(mD.name, mD, cd);
        }

        for(FieldDecl fD: cd.fieldDeclList) {
            fD.visit(this, null);
        }
        for(MethodDecl mD: cd.methodDeclList) {
            mD.visit(this, null);
        }
        si.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        md.type.visit(this, null);
        si.openScope(); //Entering a method
        for (ParameterDecl pD : md.parameterDeclList) {
            pD.visit(this, null);
        }
        si.openScope(); //Entering a statementList or Block Statement
        for (Statement s : md.statementList) {
            s.visit(this, null);
        }
        si.closeScope();
        si.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        si.addDeclaration(pd.name, pd, null);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, null); //Visit the type of the parameter Declaration
        si.addDeclaration(decl.name, decl, null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////
    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        si.openScope();
        for (Statement s :
                stmt.sl) {
            s.visit(this, null);
        }
        si.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.varDecl.visit(this, null);
        stmt.initExp.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        stmt.exp.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, null);
        for(Expression e: stmt.argList) {
            e.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if(stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.thenStmt.visit(this, null);
        if(stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.body.visit(this, null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.expr.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.visit(this, null);
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, null);
        for(Expression e: expr.argList) {
            e.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        ref.id.visit(this, null);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        Declaration theContext = ref.ref.d;
        ref.id.visit(this, null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return si.findDeclaration(id.spelling, si.currentTab.level);
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLit, Object arg) {
        return null;
    }
}
