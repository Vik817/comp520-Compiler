package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class Cmp extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.CMP;
	}

	public Cmp(RS modrmsib) {
		super(modrmsib);
	}

	public Cmp(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
}
