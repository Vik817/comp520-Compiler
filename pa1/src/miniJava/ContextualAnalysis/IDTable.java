package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;

class IDTable {

    public int level;
    public Declaration parentDecl;
    public HashMap<String, Declaration> theTable;

    public IDTable() {
        theTable = new HashMap<String, Declaration>();
    }

    public void updateTable(String s, Declaration d) {
        if(d instanceof ClassDecl) { //Level 0
            this.level = 0;
        } else {
            throw new Error();
        }

        theTable.put(s, d);
    }

    //This method below allows MemberDecls to have a parent/classDecl parameter
    public void updateTableOne(String s, Declaration d, ClassDecl classDecl) { //Know it is a MemberDecl
        this.parentDecl = classDecl;
        this.level = 1;
        theTable.put(s, d);
    }

    public void updateTableTwoPlus(String s, Declaration d, MethodDecl methodDecl) { //Know it is a LocalDecl
        this.parentDecl = methodDecl;
        this.level = 2;
        theTable.put(s, d);
    }

}
