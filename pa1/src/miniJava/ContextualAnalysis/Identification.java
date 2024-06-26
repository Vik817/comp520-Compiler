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
    String currentVar;

    //Need to implement context. How will a MethodDecl know which class it came from?
    //Could pass in an argument into visitMethodDecl taking in a classDecl
    public Identification(Package tree, ErrorReporter r) {
        this.er = r;
        this.si = new ScopedIdentification(this.er); //Maybe have to pass in the errorReporter
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
                new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null)
        );

        ClassDecl SystemClass = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(), null);
        Identifier theIdentifier = new Identifier(new Token(TokenType.ID, "_PrintStream"));
        theIdentifier.dec = PrintStreamClass;
        SystemClass.fieldDeclList.add(new FieldDecl(false, true,
                new ClassType(theIdentifier, null), "out", null));

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


        Statement currStatement = null;
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
            currStatement = s;
            s.visit(this, md); //Added md as a parameter so LocalDecls can use it for context
        }
        if(md.type.typeKind != TypeKind.VOID) {
            if(!(currStatement instanceof ReturnStmt)) {
                er.reportError("Last statement in method does not return anything");
                throw new Error();
            }
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
        //Only look for it in a class context
        //Checking if this is even a class
        //Need to check both the import levels and the actual class names
        if(si.IDTables.get(0).theTable.get(type.className.spelling) instanceof ClassDecl ||
                si.IDTables.get(1).theTable.get(type.className.spelling) instanceof ClassDecl) {
            return null;
        } else {
            er.reportError("Classtype error");
            throw new Error();
        }

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
        //Set currentVariable in usage to the varDecl's name. Will check if the stmt ends up in an IDRef
        //later, and if it does and the IdRef's identifier's name is the same, it is using
        //this variable in the same line as its declaration
        currentVar = stmt.varDecl.name;
        if(stmt.initExp.visit(this, (MethodDecl)arg) instanceof ClassDecl) {
            er.reportError("Can't assign a class to a variable");
            throw new Error();
        }
        currentVar = null;
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, (MethodDecl)arg);
        Object d = stmt.val.visit(this, (MethodDecl)arg);
        if(d instanceof MethodDecl ||
                d instanceof ClassDecl) {
            er.reportError("Cannot assign to class or method");
            throw new Error();
        }
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
        if(stmt.thenStmt instanceof VarDeclStmt) {
            er.reportError("Can't declare a variable in its own scope");
            throw new Error();
        }
        if(stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, (MethodDecl)arg);
            if(stmt.elseStmt instanceof VarDeclStmt) {
                er.reportError("Can't declare a variable in its own scope");
                throw new Error();
            }
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, (MethodDecl)arg);
        stmt.body.visit(this, (MethodDecl)arg);
        if(stmt.body instanceof VarDeclStmt) {
            er.reportError("Can't declare a variable in its own scope");
            throw new Error();
        }
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
        Object a = expr.ref.visit(this, (MethodDecl)arg);
        if(expr.ref.referenceDeclaration instanceof MethodDecl) {
            er.reportError("Can't use method here");
            throw new Error();
        }
        return a;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, (MethodDecl)arg);
        if(!(expr.ref.referenceDeclaration.type instanceof ArrayType)) {
            er.reportError("Expression's reference is not of array type");
            throw new Error();
        }
        //Make sure reference's typeDenoter is ArrayType
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
            throw new Error();
        }
        //"This" is declared in the context of the class it is not, not the method. So need the parent class of the method
        ref.referenceDeclaration = ((MethodDecl)arg).classContext; //Needed access to the current class
        //Set the type of the referenceDecl to a ClassType
        //Little weird way of fixing this but hopefully it doesn't breka anything
        ref.referenceDeclaration.type = new ClassType(new Identifier(new Token(TokenType.CLASS, ((MethodDecl)arg).classContext.name)), null);
        //ClassDecl methodsClass = this.si.findDeclaration(((MethodDecl)arg).name, null);
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        Declaration idandRefDecl = (Declaration)ref.id.visit(this, (MethodDecl)arg);
        //Eventually, VarDeclStmt will get to here, and if our IdRef's Identifier's spelling is the same
        //as the current variable we are declaring in our varDeclStmt, throw an error
        if(ref.id.spelling.equals(currentVar)) {
            er.reportError("Using variable that is currently being declared");
            throw new Error();
        }
        ref.referenceDeclaration = idandRefDecl; //Set a reference Declaration to use as context for our QualRef
        if(ref.referenceDeclaration instanceof ClassDecl) {
            //Now we need to find where the id was declared in our reference's context
            //Could be either a Field or Method Decl
            //Or it could just be the class itself, which if it is, we just end up returning the idandRefDecl
            if(ref.referenceDeclaration.name.equals(ref.id.spelling)) {
                ClassDecl contextClass = (ClassDecl) ref.referenceDeclaration;
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
                if(member != null) {
                    return member;
                }
            }

        }
        if(((MethodDecl)arg).isStatic) {
            if(idandRefDecl instanceof MemberDecl) {
                if(!((MemberDecl)idandRefDecl).isStatic) {
                    er.reportError("Using non-static parameter in static context");
                    throw new Error();
                }
            }
        }
        //Decided to return theidandRefDecl
        //Necessary in order to figure out if, in one of our VarDeclStmts, if we are assinging something
        //to just a class, which wouldn't work
        return idandRefDecl;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, (MethodDecl)arg); //Will continue to visit QualRefs until It ends in an identifier
        //.println(ref.ref.referenceDeclaration.name);
        if(ref.ref.referenceDeclaration == null) {
            //System.out.println("what");
            er.reportError("No reference declaration error");
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
            if(ref.ref.referenceDeclaration instanceof MethodDecl) {
                er.reportError("Left of QRef is a method");
                throw new Error();
            }

            if(member == null) {
                er.reportError("Reference not found");
                throw new Error();
            }
            //Need to check if the member we found is private
            //If it is, we use the method decl that was passed in and see if its class as well as the member's
            //class are the same. If they aren't, we cannot access this member.
            if(member.isPrivate && !(((MethodDecl)arg).classContext.name.equals(contextClass.name))) {
                er.reportError("Cannot access private member");
                throw new Error();
            }
            if(((MethodDecl)arg).isStatic) {
                if(!member.isStatic) {
                    //System.out.println(member.name);
                    er.reportError("Accessing nonstatic member from a static class");
                    throw new Error();
                }
            }

            //Need to update the references' declarations
            ref.id.dec = member;
            ref.referenceDeclaration = ref.id.dec; // Have it for now to reset the referenceDecl but check later

        } else if(theContext instanceof MemberDecl) {
            MemberDecl contextMember = (MemberDecl) theContext;
            if(!(contextMember.type.typeKind == TypeKind.CLASS)) { //The context of our member has to be of type Class
                er.reportError("Accessing without use of a class");
                throw new Error();
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
                //This checks if the current QRef's left reference is a method. If so,
                //there is an issue as we can't have methodDecl.a qref
                if(ref.ref.referenceDeclaration instanceof MethodDecl) {
                    er.reportError("Left of QRef is a method");
                    throw new Error();
                }

                if(member == null) {
                    er.reportError("Reference not found");
                    throw new Error();
                }
                if(member.isPrivate && !(((MethodDecl)arg).classContext.name.equals(classOrigin.name))) {
                    er.reportError("Cannot access private member");
                    throw new Error();
                }
                ref.id.dec = member;
                ref.referenceDeclaration = ref.id.dec;
            }

        } else if(theContext instanceof LocalDecl) {
            LocalDecl contextLocal = (LocalDecl) theContext;
            if(!(contextLocal.type.typeKind == TypeKind.CLASS)) { //The context of our member has to be of type Class
                er.reportError("Accessing without use of a class");
                throw new Error();
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
                if(ref.ref.referenceDeclaration instanceof MethodDecl) {
                    er.reportError("Left of QRef is a method");
                    throw new Error();
                }

                if(member == null) {
                    er.reportError("Reference not found");
                    throw new Error();
                }
                if(member.isPrivate && !(((MethodDecl)arg).classContext.name.equals(classOrigin.name))) {
                    er.reportError("Cannot access private member");
                    throw new Error();
                }
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
