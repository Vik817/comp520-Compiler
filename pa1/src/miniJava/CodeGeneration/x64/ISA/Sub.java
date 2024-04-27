package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class Sub extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.SUB;
	}
	
	public Sub(RS modrmsib) {
		super(modrmsib);
	}

	public Sub(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
}
