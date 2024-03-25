package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import miniJava.ContextualAnalysis.*;
import miniJava.ErrorReporter;

public class ScopedIdentification {

    public Stack<IDTable> IDTables;
    public static int currentLevel = -1;
    public IDTable currentTab;
    public ErrorReporter eReporter;

    public ScopedIdentification(ErrorReporter report) {
        this.eReporter = report;
        this.IDTables = new Stack<>();
    }

    /*
    FieldDeclList fList = new FieldDeclList();
        fList.add(new FieldDecl(false, true, new BaseType(TypeKind.CLASS, null), "out", null));
        ClassDecl theSystem = new ClassDecl("System", fList, null, null);
        IDTable system = new IDTable();
        system.updateTable("System", theSystem);
        si.IDTables.add(system);

        MethodDeclList mList = new MethodDeclList();
        FieldDecl fForM = new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null);
        ParameterDeclList pList = new ParameterDeclList();
        pList.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        mList.add(new MethodDecl(fForM, pList, null, null));

        ClassDecl stringClass = new ClassDecl("String", null, null, null);
     */
    public void openScope() {
        currentTab = new IDTable();
        currentLevel = currentTab.level;
        this.IDTables.push(currentTab);
    }

    public void closeScope() {
        this.IDTables.pop();
    }

    public void addDeclaration(String a, Declaration d, Declaration contextDecl) {
        //Check if Declaration is a MemberDecl, if so, we can use cD. Else, disregard it
        if(d instanceof ClassDecl) { //Know this is at level 0.
            if(d.name.equals("String") || d.name.equals("PrintStream") || d.name.equals("System")) {
                if(this.currentTab.level == 1) {
                    eReporter.reportError("Class already declared");
                    throw new IdentificationError();
                }
            }
            if(this.currentTab.level != 0) {
                eReporter.reportError("Not on level 0");
                throw new IdentificationError();
            } else {
                if(this.currentTab.theTable.containsKey(a)) {
                    eReporter.reportError("Class already declared");
                    throw new IdentificationError();
                }
                this.currentTab.updateTable(a, d); //Adds a classDecl
            }
        } else if(d instanceof MemberDecl) { //Know this is at level 1. Need to check if it is private
            if(contextDecl != null) {
                for(IDTable t: IDTables) {
                    if(t.level == 1) { //Checks only IDTable 1
                        if(t.theTable.containsKey(a)) { //Checks if this table has the element or not
                            eReporter.reportError("Member already declared");
                            throw new IdentificationError(); //Make this an identification error
                        }
                    }
                }
                this.currentTab.updateTableOne(a, d, (ClassDecl)contextDecl); //Adds a Field or MethodDecl with parent Class
            } else {
                eReporter.reportError("No context provided");
                throw new IdentificationError(); //No context given
            }
        } else {
            IDTable newTab = new IDTable();
            newTab.updateTable(a, d); //Creates a new IDTable we can run tests with to see if it alrdy exists at same or higher scope
            int newTabScope = newTab.level;
            for(IDTable tab: IDTables) { //For each table in IDTables
                if(tab.level >= newTabScope) { //Iterate through tables with its scope level or higher
                    if(tab.theTable.containsKey(a)) {
                        //System.out.println(tab.theTable);
                        eReporter.reportError("Local Declaration already declared");
                        throw new IdentificationError(); //Make this an identification error
                    }
                }
            } //If it makes it through the for loop, then we can add it
            this.currentTab.updateTableTwoPlus(a, d, (MethodDecl)contextDecl);
        }


    }

    public Declaration findDeclaration(String a, Declaration methodContext) {

        Declaration decl = null;
        for(int i = IDTables.size() - 1; i >= 0; i--) {
            if(IDTables.get(i).theTable.containsKey(a)) {
                decl = IDTables.get(i).theTable.get(a);
            }
        }

        if(decl == null) {
            eReporter.reportError("Declaration does not exist");
            throw new IdentificationError();
        } else if(methodContext instanceof ClassDecl) {
            if(!(decl instanceof ClassDecl)) {
                eReporter.reportError("Method doesn't come from a class");
                throw new IdentificationError();
            }
        } else if(methodContext instanceof MethodDecl) {
            MethodDecl mContext = (MethodDecl) methodContext;
            if(mContext != null) {
                if(mContext.isPrivate && decl instanceof MemberDecl) {
                    if(!((MemberDecl) decl).isStatic) { //If our decl is not static, we can't use it in our static method
                        eReporter.reportError("Cannot access this");
                        throw new IdentificationError();
                    }
                }
            }
        }
        //Need to check the case where our current method is static
        //If our current method is static, it can only access stuff that is also static. Check if Decl is a MemberDecl and static

        return decl;

//        int closestScope = scopeContext;
//        Declaration decl = null;
//        boolean found = false;
//        while(closestScope >= 0) {
//            for(IDTable table: IDTables) {
//                if(table.level == closestScope) { //Iterates through every IDTable of the same scope
//                    if(table.theTable.containsKey(a)) { //Checks if in that scope there is an Identifier Declaration...
//                        // ...pair with the same spelling
//                        found = true; //Indicates the declaration has been found
//                        decl = table.theTable.get(a); //Sets decl = the found declaration
//                        break;
//                    }
//                }
//            }
//            if(found) {
//                break;
//            }
//            closestScope--;
//        }
//        if(closestScope != -1) {
//            return decl;
//        } else {
//            throw new IdentificationError(); //Identification Error
//        }
    }


    class IdentificationError extends Error {
        private static final long serialVersionUID = -6461942006097999362L;


    }
}
