package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class Add extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.ADD;
	}
	
	public Add(RS modrmsib) {
		super(modrmsib);
	}

	public Add(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
}
