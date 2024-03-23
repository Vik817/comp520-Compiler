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

    public void addDeclaration(String a, Declaration d, ClassDecl cD) {
        //Check if Declaration is a MemberDecl, if so, we can use cD. Else, disregard it
        if(d instanceof MemberDecl) { //Know this is at level 1. Need to check if it is private
            if(cD != null) {
                for(IDTable t: IDTables) {
                    if(t.level == 1) { //Checks only IDTable 1
                        if(t.theTable.get(a) != null) { //Checks if this table has the element or not
                            throw new Error(); //Make this an identification error
                        }
                    }
                }
                this.currentTab.updateTableOne(a, d, cD);
            }
        } else {
            IDTable newTab = new IDTable();
            newTab.updateTable(a, d); //Creates a new IDTable we can run tests with to see if it alrdy exists at same or higher scope
            int newTabScope = newTab.level;
            for(IDTable tab: IDTables) { //For each table in IDTables
                if(tab.level >= newTabScope) { //Iterate through tables with its scope level or higher
                    if(tab.theTable.get(a) != null) {
                        throw new Error(); //Make this an identification error
                    }
                }
            } //If it makes it through the for loop, then we can add it
            this.currentTab.updateTable(a, d);
        }


    }

    public Declaration findDeclaration(String a, int scopeContext) {
        int closestScope = scopeContext;
        Declaration decl = null;
        boolean found = false;
        while(closestScope >= 0) {
            for(IDTable table: IDTables) {
                if(table.level == closestScope) { //Iterates through every IDTable of the same scope
                    if(table.theTable.containsKey(a)) { //Checks if in that scope there is an Identifier Declaration...
                        // ...pair with the same spelling
                        found = true; //Indicates the declaration has been found
                        decl = table.theTable.get(a); //Sets decl = the found declaration
                        break;
                    }
                }
            }
            if(found) {
                break;
            }
            closestScope--;
        }
        if(closestScope != -1) {
            return decl;
        } else {
            throw new Error(); //Identification Error
        }
    }


}
