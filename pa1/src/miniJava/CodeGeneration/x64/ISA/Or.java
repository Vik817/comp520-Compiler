package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class Or extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.OR;
	}
	
	public Or(RS modrmsib) {
		super(modrmsib);
	}

	public Or(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
}
