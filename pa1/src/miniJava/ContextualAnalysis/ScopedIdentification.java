package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import miniJava.ContextualAnalysis.*;

public class ScopedIdentification {

    public Stack<IDTable> IDTables;
    public static int currentLevel = -1;
    public IDTable currentTab;

    public ScopedIdentification() {
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
            if(this.currentTab.level != 0) {
                throw new IdentificationError();
            } else {
                if(this.currentTab.theTable.containsKey(a)) {
                    throw new IdentificationError();
                }
                this.currentTab.updateTable(a, d); //Adds a classDecl
            }
        } else if(d instanceof MemberDecl) { //Know this is at level 1. Need to check if it is private
            if(contextDecl != null) {
                for(IDTable t: IDTables) {
                    if(t.level == 1) { //Checks only IDTable 1
                        if(t.theTable.containsKey(a)) { //Checks if this table has the element or not
                            throw new IdentificationError(); //Make this an identification error
                        }
                    }
                }
                this.currentTab.updateTableOne(a, d, (ClassDecl)contextDecl); //Adds a Field or MethodDecl with parent Class
            } else {
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
                        throw new IdentificationError(); //Make this an identification error
                    }
                }
            } //If it makes it through the for loop, then we can add it
            this.currentTab.updateTableTwoPlus(a, d, (MethodDecl)contextDecl);
        }


    }

    public Declaration findDeclaration(String a, MethodDecl methodContext) {

        Declaration decl = null;
        for (IDTable table: IDTables) {
            if(table.theTable.containsKey(a)) {
                decl = table.theTable.get(a);
            }
        }

        if(decl == null) {
            throw new IdentificationError();
        }
        //Need to check the case where our current method is static
        //If our current method is static, it can only access stuff that is also static. Check if Decl is a MemberDecl and static
        else if(methodContext != null) {
            if(methodContext.isPrivate && decl instanceof MemberDecl) {
                if(!((MemberDecl) decl).isStatic) { //If our decl is not static, we can't use it in our static method
                    throw new IdentificationError();
                }
            }
        }
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
