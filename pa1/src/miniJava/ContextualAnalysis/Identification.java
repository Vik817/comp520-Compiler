package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.*;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class Identification implements Visitor {

    private ScopedIdentification si;
    private ErrorReporter er;
    Package pack;

    //Need to implement context. How will a MethodDecl know which class it came from?
    //Could pass in an argument into visitMethodDecl taking in a classDecl
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
        ///Lets add IMPORTS here
        si.openScope();
        ClassDecl StringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);

        ClassDecl PrintStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(), new MethodDeclList(), null);
        PrintStreamClass.methodDeclList.add(
                new MethodDecl(new FieldDecl(false, false,
                        new BaseType(TypeKind.VOID, null),
                        "println", null),
                        new ParameterDeclList(),
                        new StatementList(), null)
        );
        PrintStreamClass.methodDeclList.get(0).parameterDeclList.add(
                new ParameterDecl(new BaseType(TypeKind.INT, null), "_PrintStream", null)
        );

        ClassDecl SystemClass = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(), null);
        SystemClass.fieldDeclList.add(new FieldDecl(false, true,
                new ClassType(new Identifier(new Token(TokenType.CLASS, "_PrintStream")), null), "out", null));

        si.addDeclaration("System", SystemClass, null);
        si.addDeclaration("_PrintStream", PrintStreamClass, null);
        si.addDeclaration("String", StringClass, null);





        si.openScope();
        for(ClassDecl cD: prog.classDeclList) { //Add all classDecls in the Package's AST to level 0 IDTable
            si.addDeclaration(cD.name, cD, null);
        }
        for(ClassDecl cD: prog.classDeclList) {
            cD.visit(this, null);
        }
        si.closeScope();
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
            fD.visit(this, cd);
        }
        for(MethodDecl mD: cd.methodDeclList) {
            mD.visit(this, cd);
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
        if(arg != null) {
            md.classContext = (ClassDecl)arg;
        }
        md.type.visit(this, null);
        si.openScope(); //Entering a method
        for (ParameterDecl pD : md.parameterDeclList) {
            pD.visit(this, md); //Added md as a parameter so LocalDecls can use it for context
        }
        si.openScope(); //Entering a statementList or Block Statement
        for (Statement s : md.statementList) {
            s.visit(this, md); //Added md as a parameter so LocalDecls can use it for context
        }
        si.closeScope();
        si.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        si.addDeclaration(pd.name, pd, (MethodDecl)arg); //Gives the parameterDecl some context of which method it is in
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, (MethodDecl)arg); //Visit the type of the parameter Declaration
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
        si.findDeclaration(type.className.spelling, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, (MethodDecl)arg);
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
            s.visit(this, (MethodDecl)arg);
        }
        si.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.varDecl.visit(this, (MethodDecl)arg);
        stmt.initExp.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, (MethodDecl)arg);
        stmt.val.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, (MethodDecl)arg);
        stmt.ix.visit(this, (MethodDecl)arg);
        stmt.exp.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, (MethodDecl)arg);
        for(Expression e: stmt.argList) {
            e.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if(stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, (MethodDecl)arg);
        stmt.thenStmt.visit(this, (MethodDecl)arg);
        if(stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, (MethodDecl)arg);
        stmt.body.visit(this, (MethodDecl)arg);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.operator.visit(this, (MethodDecl)arg);
        expr.expr.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.operator.visit(this, (MethodDecl)arg);
        expr.left.visit(this, (MethodDecl)arg);
        expr.right.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.visit(this, (MethodDecl)arg);
        expr.ixExpr.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, (MethodDecl)arg);
        for(Expression e: expr.argList) {
            e.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, (MethodDecl)arg);
        expr.sizeExpr.visit(this, (MethodDecl)arg);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        //Keyword "this" does not make sense in a static context
        if(((MethodDecl)arg).isStatic) {
            er.reportError("Using 'this' in a static context'");
        }
        //"This" is declared in the context of the class it is not, not the method. So need the parent class of the method
        ref.referenceDeclaration = ((MethodDecl)arg).classContext; //Needed access to the current class
        //ClassDecl methodsClass = this.si.findDeclaration(((MethodDecl)arg).name, null);
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        Declaration idandRefDecl = (Declaration)ref.id.visit(this, (MethodDecl)arg);
        ref.referenceDeclaration = idandRefDecl; //Set a reference Declaration to use as context for our QualRef
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, (MethodDecl)arg); //Will continue to visit QualRefs until It ends in an identifier
        if(ref.ref.referenceDeclaration == null) {
            throw new Error();
        }
        Declaration theContext = ref.ref.referenceDeclaration; //Provide the current context of the referencce

        //Now need to check where this context is
        if(theContext instanceof ClassDecl) { //The thing using the context is a MemberDecl
            //Now we need to find where the id was declared in our reference's context
            //Could be either a Field or Method Decl
            ClassDecl contextClass = (ClassDecl) theContext;
            MemberDecl member = null;
            boolean declarationFound = false;
            while(!declarationFound) {
                for(FieldDecl f: contextClass.fieldDeclList) {
                    if(ref.id.spelling.equals(f.name)) {
                        member = f;
                        declarationFound = true;
                        break;
                    }
                }
                if(declarationFound) {
                    break;
                }
                for(MethodDecl m: contextClass.methodDeclList) {
                    if(ref.id.spelling.equals(m.name)) {
                        member = m;
                        break;
                    }
                }
                break;
            }

            if(member == null) {
                er.reportError("Reference not found");
            }
            if(member.isPrivate) {
                er.reportError("Cannot access private member");
            }
            if(((MethodDecl)arg).isStatic) {
                if(!member.isStatic) {
                    er.reportError("Accessing nonstatic member from a static class");
                }
            }

            //Need to update the references' declarations
            ref.id.dec = member;
            ref.referenceDeclaration = ref.id.dec; // Have it for now to reset the referenceDecl but check later

        } else if(theContext instanceof MemberDecl) {
            MemberDecl contextMember = (MemberDecl) theContext;
            if(!(contextMember.type.typeKind == TypeKind.CLASS)) { //The context of our member has to be of type Class
                er.reportError("Accessing without use of a class");
            } else {
                //Need the class it is from in order to look for the declaration
                ClassDecl classOrigin = (ClassDecl)si.findDeclaration(
                        ((ClassType)contextMember.type).className.spelling, (MethodDecl)arg);
                MemberDecl member = null;
                boolean declarationFound = false;
                while(!declarationFound) {
                    for(FieldDecl f: classOrigin.fieldDeclList) {
                        if(ref.id.spelling.equals(f.name)) {
                            member = f;
                            declarationFound = true;
                            break;
                        }
                    }
                    if(declarationFound) {
                        break;
                    }
                    for(MethodDecl m: classOrigin.methodDeclList) {
                        if(ref.id.spelling.equals(m.name)) {
                            member = m;
                            break;
                        }
                    }
                    break;
                }

                if(member == null) {
                    er.reportError("Reference not found");
                }
                if(member.isPrivate) {
                    er.reportError("Cannot access private member");
                }
                if(((MethodDecl)arg).isStatic) {
                    if(!member.isStatic) {
                        er.reportError("Accessing nonstatic member from a static class");
                    }
                } //Might not need this
                ref.id.dec = member;
                ref.referenceDeclaration = ref.id.dec;
            }
        } else if(theContext instanceof LocalDecl) {
            LocalDecl contextLocal = (LocalDecl) theContext;
            if(!(contextLocal.type.typeKind == TypeKind.CLASS)) { //The context of our member has to be of type Class
                er.reportError("Accessing without use of a class");
            } else {
                //Need the class it is from in order to look for the declaration
                ClassDecl classOrigin = (ClassDecl)si.findDeclaration(
                        ((ClassType)contextLocal.type).className.spelling, (MethodDecl)arg);
                MemberDecl member = null;
                boolean declarationFound = false;
                while(!declarationFound) {
                    for(FieldDecl f: classOrigin.fieldDeclList) {
                        if(ref.id.spelling.equals(f.name)) {
                            member = f;
                            declarationFound = true;
                            break;
                        }
                    }
                    if(declarationFound) {
                        break;
                    }
                    for(MethodDecl m: classOrigin.methodDeclList) {
                        if(ref.id.spelling.equals(m.name)) {
                            member = m;
                            break;
                        }
                    }
                    break;
                }

                if(member == null) {
                    er.reportError("Reference not found");
                }
                if(member.isPrivate) {
                    er.reportError("Cannot access private member");
                }
                if(((MethodDecl)arg).isStatic) {
                    if(!member.isStatic) {
                        er.reportError("Accessing nonstatic member from a static class");
                    }
                } //Might not need this
                ref.id.dec = member;
                ref.referenceDeclaration = ref.id.dec;
            }

        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return si.findDeclaration(id.spelling, (MethodDecl)arg);
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
