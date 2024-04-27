package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CodeGenerator implements Visitor<Object, Object> {
	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section
	private int rbpOffset = -8;
	private int pdOffset = 16;
	private int classRuntimeEntity = 0;

	private int staticOffset = 0;

	private int fieldDeclOffset = 0;

	private HashMap<String, List<Instruction>> patches = new HashMap<>();
	boolean hasRequiredMainMethod = false;
	int mainMethodAddress = -1;

	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}

	public void parse(Package prog) {
		_asm = new InstructionList();
		_asm.markOutputStart();

		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.

		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX

		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx

		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed

		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );

		prog.visit(this,null);
		_asm.outputFromMark();
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}

	//////////////////////////////////////////////////////////////
	// START OF VISITOR METHODS //
	//////////////////////////////////////////////////////////////
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO: visit relevant parts of our AST
		for(ClassDecl cD : prog.classDeclList) {
			cD.visit(this, null);
		}
		if(!hasRequiredMainMethod) {
			_errors.reportError("Does not have a public static void main method");
			throw new Error();
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {

		cd.runTimeOffset = classRuntimeEntity;
		classRuntimeEntity += 8;
		for(FieldDecl fD : cd.fieldDeclList) {
			fD.visit(this, cd);
		}
		for(MethodDecl mD : cd.methodDeclList) {
			mD.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {

		if(fd.isStatic) {
			//Need to give it the offset of where the static variables are located
			fd.offset = staticOffset;
			staticOffset += 8;
		} else {
			//Need to give fd some offset
			fd.offset = fieldDeclOffset;
			fieldDeclOffset += 8;

		}
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		if(md.name.equals("println") && md.classContext.name.equals("_PrintStream")) {
			makePrintln();
			return null;
		}

		md.runtimeAddress = _asm.getSize(); //Gives the start address of where this method is
		//This is the index in the _asm list where the methodDecl is
		//Can access the asm list's index here, and get the address at that index to get the method address

		_asm.add(new Push(Reg64.RBP)); //push rbp
		_asm.add(new Mov_rmr(new RS(Reg64.RBP, Reg64.RSP))); //mov rbp, rsp


		//If Im at my main method
		if(!md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID && md.name.equals("main")) {
			if(md.parameterDeclList.size() == 1) { //Checks if it only has a one parameter which should be String[]
				ParameterDecl currPD = md.parameterDeclList.get(0);
				if(currPD.type instanceof ArrayType) {
					if(((ClassType) ((ArrayType)currPD.type).eltType).className.spelling.equals("String")) { //Checks if it is String
						if(currPD.name.equals("args")) {
							hasRequiredMainMethod = true;
							mainMethodAddress = md.runtimeAddress;
						}
					}

				}
			}
		}

		for(ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, md);
		}
		for(Statement s : md.statementList) {
			s.visit(this, md);
		}


		if(patches.containsKey(md.name)) {
			for(Instruction i : patches.get(md.name)) {
				_asm.patch(i.listIdx, new Call(i.startAddress, md.runtimeAddress));
			}
		}

		rbpOffset = 0; //reset the offset
		pdOffset = 0;
		_asm.add(new Mov_rmr(new RS(Reg64.RSP, Reg64.RBP))); //mov rsp, rbp
		_asm.add(new Pop(Reg64.RBP)); //pop rbp
		_asm.add(new Ret()); //ret

		if(hasRequiredMainMethod) { //Im at the end of my main Method
			_asm.add(new Xor( new RS(Reg64.RDI, Reg64.RDI)));
			_asm.add(new Mov_ri64(Reg64.RAX, 0));
			_asm.add(new Syscall());
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		//Need to check if "this" exists
		pd.offsetStore = pdOffset;
		_asm.add(new Push(0)); //push 0 on the stack to account for a parameter
		pdOffset += 8;
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.offsetStore = rbpOffset; //offset of the decl is set to the current rbpOffset
		_asm.add(new Push(0)); //moves rsp and is a placeholder for the variable's value
		rbpOffset -= 8; //set the rbpOffset to the next byte (minus 8)

		return null;
	}

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
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		for(Statement st : stmt.sl) {
			st.visit(this, null);
		}
		//Need to remove everything that was put on the stack in this method
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		_asm.add(new Pop(Reg64.RAX)); //pop RAX (pops initExp value from stack and puts in RAX)
		//My variable is at RBP + stmt.vardecl.offset
		//I want to take value in RAX and store it at that memory location
		//mov RMR: RBP + the offset, RAX
		_asm.add(new Mov_rmr(new RS(Reg64.RBP, stmt.varDecl.offsetStore, Reg64.RAX)));
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, Boolean.TRUE); //passing True will give the address of the reference
		stmt.val.visit(this, null);
		_asm.add(new Pop(Reg64.RCX)); //has value
		_asm.add(new Pop(Reg64.RAX)); //has reference's address

		// [RAX] = RCX
		// mov [RAX] RCX //Moves the value into the reference's location
		_asm.add(new Mov_rmr(new RS(Reg64.RAX, 0, Reg64.RCX)));
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.exp.visit(this, null);
		stmt.ix.visit(this, null);

		_asm.add(new Pop(Reg64.RCX)); //has index
		_asm.add(new Pop(Reg64.RBX)); //has expression/value
		_asm.add(new Pop(Reg64.RAX)); //has reference

		//[rdisp+ridx*mult+disp],r
		//rdisp = RAX
		//ridx = RCX
		//disp = 0
		//r = RBX
		_asm.add(new Mov_rmr(new RS(Reg64.RAX, Reg64.RCX, 8, 0, Reg64.RBX)));
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {


		//Visit each argument in reverse order (so they get pushed on the stack in reverse)
		for(int i = stmt.argList.size() -1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, null);
		}
		if(stmt.methodRef instanceof IdRef) {
			MethodDecl mD = (MethodDecl)((IdRef)stmt.methodRef).id.dec;

			if(mD.isStatic) {
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);
					//Need to patch
				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}

			} else {
				(stmt.methodRef).visit(this, true); //Should push "this" on the stack
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);//Adds the instruction to a patch List
					//Need to pass in what method it is from

				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			}
		} else if(stmt.methodRef instanceof QualRef) {
			MethodDecl mD = (MethodDecl)((QualRef)stmt.methodRef).id.dec;
			if(mD.isStatic) {
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);
					//Need to patch
				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			} else {
				(stmt.methodRef).visit(this, true); //Should push "this" on the stack
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);//Adds the instruction to a patch List
					//Need to pass in what method it is from

				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			}
		}

		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		stmt.returnExpr.visit(this, null);
		//Do i add a Ret() instruction
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);

		_asm.add(new Pop(Reg64.RAX)); //has condition

		//Should check if the condition is true, and if so, visit the thenStmt
		//If it's not true, then if there's an elseStmt, visit that
		//The number 1 should be pushed if it is true

		_asm.add(new Cmp(new RS(Reg64.RAX, false), 1)); //Compares condition to true
		int currAdd = _asm.getSize();
		CondJmp cJ = new CondJmp(Condition.E, 0); //Jumps if equal, need to patch later
		_asm.add(cJ);
		//patches.add(cJ);

		if(stmt.elseStmt != null) {
			int otherAdd = _asm.getSize();
			Jmp j = new Jmp(0);
			stmt.elseStmt.visit(this, null);
			_asm.patch(j.listIdx, new Jmp(otherAdd, _asm.getSize(), false));
		}
		//Here is where the thenStmt will be
		stmt.thenStmt.visit(this, null);
		_asm.patch(cJ.listIdx, new CondJmp(Condition.E, currAdd, _asm.getSize(), false)); //Patches condJmp

		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RAX)); //has condition
		_asm.add(new Cmp(new RS(Reg64.RAX, Reg64.RAX), 1)); //Compares condition to true

		int currAdd = _asm.getSize();
		CondJmp cJ = new CondJmp(Condition.NE, 0); //Jumps if not equal, need to patch later
		_asm.add(cJ);
		stmt.body.visit(this, null);
		_asm.add(new Jmp(_asm.getSize(), cJ.startAddress, false)); //Is it AsByte? Idk if that's true
		//Should jump back to the comparison statement

		_asm.patch(cJ.listIdx, new CondJmp(Condition.NE, currAdd, _asm.getSize(), false)); //Patches it to jump to after...
		//...the body is done
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		Operator op = expr.operator;
		_asm.add(new Pop(Reg64.RAX));
		if(op.spelling.equals("-")) {
			_asm.add(new Neg(new RS(Reg64.RAX, false))); //Need to check this
		} else if(op.spelling.equals("!")) {
			_asm.add(new Not(new RS(Reg64.RAX, false))); //Need to check this
		}
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		//Get the operator
		//Visit the left statement, and everything from that side will be put onto the stack
		//Visit the right statement, and everything from that side will be put onto the stack
		//Pop what is from the stack into rax and rcx, then do the operation based on expr.operator
		//push the result onto the stack
		Operator op = expr.operator;
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		_asm.add(new Pop(Reg64.RCX)); //puts right in rcx
		_asm.add(new Pop(Reg64.RAX)); //puts left in rax

		if(op.spelling.equals("+")) {
			_asm.add(new Add(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else if (op.spelling.equals("-")) {
			_asm.add(new Sub(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else if(op.spelling.equals("*")) {
			_asm.add(new Imul(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else if(op.spelling.equals("/")) {
			_asm.add(new Idiv(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else if(op.spelling.equals("||")) {
			_asm.add(new Or(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else if(op.spelling.equals("&&")) {
			_asm.add(new And(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new Push(Reg64.RAX));
		} else { //Any comparison operator ( >, >=, <, <=, ==, != )
			_asm.add(new Xor(new RS(Reg64.RBX, Reg64.RBX))); //Zeros register
			_asm.add(new Cmp(new RS(Reg64.RAX, Reg64.RCX)));
			_asm.add(new SetCond(Condition.getCond(op), Reg8.BL)); //Result is in RBX
			_asm.add(new Push(Reg64.RBX));
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);

		_asm.add(new Pop(Reg64.RCX)); //has index
		_asm.add(new Pop(Reg64.RAX)); //has reference

		_asm.add(new Push(new RS(Reg64.RAX, Reg64.RCX, 8, 0)));

		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {

		//Visit each argument in reverse order (so they get pushed on the stack in reverse)
		for(int i = expr.argList.size() -1; i >= 0; i--) {
			expr.argList.get(i).visit(this, null);
		}
		if(expr.functionRef instanceof IdRef) {
			MethodDecl mD = (MethodDecl)((IdRef)expr.functionRef).id.dec;

			if(mD.isStatic) {
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);
					//Need to patch
				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}

			} else {
				(expr.functionRef).visit(this, true); //Should push "this" on the stack
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);//Adds the instruction to a patch List
					//Need to pass in what method it is from

				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			}
		} else if(expr.functionRef instanceof QualRef) {
			MethodDecl mD = (MethodDecl)((QualRef)expr.functionRef).id.dec;
			if(mD.isStatic) {
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);
					//Need to patch
				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			} else {
				(expr.functionRef).visit(this, true); //Should push "this" on the stack
				if(mD.runtimeAddress == -1) {
					if(!patches.containsKey(mD.name)) {
						patches.put(mD.name, new ArrayList<>());
					}
					int currAdd = _asm.getSize();
					Instruction a = new Call(currAdd, 0);
					_asm.add(a);
					patches.get(mD.name).add(a);//Adds the instruction to a patch List
					//Need to pass in what method it is from

				} else {
					int currAdd = _asm.getSize();
					_asm.add(new Call(currAdd, mD.runtimeAddress));
				}
			}
		}

//		_asm.add(new Pop(Reg64.RAX)); //saves the value from the expression
//		_asm.add(new Mov_rmr(new RS(Reg64.RSP, Reg64.RBP))); //mov rsp, rbp
//		_asm.add(new Pop(Reg64.RBP)); //pop rbp
//		_asm.add(new Push(Reg64.RAX)); //pushes back the value we calculated
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		_asm.add(new Push(0)); //push 0 Allocates space for pointer for "new A" referring to the class fd is in
		makeMalloc(); //rax has pointer to the newly allocated memory
		_asm.add(new Mov_rmr(new RS(Reg64.RBP, -8, Reg64.RAX)));
		//mov [rbp-8], rax :: This will store the pointer to the newly allocated memory
		//in the correct location


		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		_asm.add(new Push(0)); //push 0 Allocates space for pointer for "new A" referring to the class fd is in
		makeMalloc(); //rax has pointer to the newly allocated memory
		_asm.add(new Mov_rmr(new RS(Reg64.RBP, -8, Reg64.RAX)));
		//mov [rbp-8], rax :: This will store the pointer to the newly allocated memory
		//in the correct location
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		_asm.add(new Push(new RS(Reg64.RBP, 16))); //this is always at RBP+16
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if(ref.id.dec instanceof LocalDecl) { //offset from RBP
			LocalDecl lD = (LocalDecl) ref.id.dec;
			if(arg instanceof Boolean) {
				_asm.add(new Lea(new RS(Reg64.RBP, lD.offsetStore, Reg64.RAX)));
				_asm.add(new Push(Reg64.RAX));
			} else {
				_asm.add(new Push(new RS(Reg64.RBP, lD.offsetStore)));
			}
		} else if(ref.id.dec instanceof FieldDecl) { //Im in a method
			FieldDecl fD = (FieldDecl) ref.id.dec;
			_asm.add(new Mov_rmr(new RS(Reg64.RBP, 16, Reg64.RAX))); //moves RBP+16 (this) into RAX
			if(arg instanceof Boolean) {
				_asm.add(new Lea(new RS(Reg64.RAX, fD.offset, Reg64.RAX)));
				//mov RAX + fd.offset (which will give us our id's location) into RAX
				_asm.add(new Push(Reg64.RAX));
			} else {
				_asm.add(new Push(new RS(Reg64.RAX, fD.offset)));
			}
		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		ref.ref.visit(this, Boolean.TRUE);
		//visit LHS
		//pop into a register?
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Add(new RS(Reg64.RAX, false), ((FieldDecl)ref.id.dec).offset));
		//gets the location of the id
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		_asm.add(new Push(Integer.parseInt(num.spelling))); //push the integer onto the stack
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if(bool.spelling.equals("true")) {
			_asm.add(new Push(1));
		} else {
			_asm.add(new Push(0));
		}
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLit, Object arg) {
		return null;
	}

	//////////////////////////////////////////////////////////////
	// END OF VISITOR METHODS //
	//////////////////////////////////////////////////////////////

	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), mainMethodAddress); // TODO: set the location of the main method
	}

	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new RS(Reg64.RAX,true),0x09) ); // mmap

		_asm.add( new Xor(new RS(Reg64.RDI,Reg64.RDI))); // addr=0
		_asm.add( new Mov_rmi(new RS(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(new RS(Reg64.RDX,true),0x03)); // prot read|write
		_asm.add( new Mov_rmi(new RS(Reg64.R10,true),0x22)); // flags= private, anonymous
		_asm.add( new Mov_rmi(new RS(Reg64.R8, true),-1)); // fd= -1
		_asm.add( new Xor(new RS(Reg64.R9,Reg64.R9))); // offset=0
		_asm.add( new Syscall() );

		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}

	private int makePrintln() {
		// TODO: how can we generate the assembly to println?
		return -1;
	}


	//thisRef never appears in a callstmt
	//If functionRef is an instatnce of a QualRef, cast to qual ref, and get that ref's .id.decl
	//If it is idRef, get idRef.id.decl

	//If md.runtimeAddress is -1, we have to patch?

	//i = argList.size() - 1, loop from the last argument backwards
	//getArgList, visit the Expressions
	//That way, first variable is last expression evaluated on the stack
	//After ur done visiting expressions, you generate call instructions

	//Call(i.startAddress to md.startAddress)
}
