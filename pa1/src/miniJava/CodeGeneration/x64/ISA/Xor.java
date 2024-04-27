package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.RS;

public class Xor extends SimpleMathInstruction {
	@Override
	protected SimpleMathOp _thisOp() {
		return SimpleMathOp.XOR;
	}
	
	public Xor(RS modrmsib) {
		super(modrmsib);
	}

	public Xor(RS modrmsib, int imm) {
		super(modrmsib,imm);
	}
}
