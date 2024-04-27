package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class And extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.AND;
	}

	public And(RS modrmsib) {
		super(modrmsib);
	}

	public And(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
	
	public And(RS modrmsib, int imm, boolean signExtend) {
		super(modrmsib,imm,signExtend);
	}
}
