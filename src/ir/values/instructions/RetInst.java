package ir.values.instructions;

import ir.types.Type;

public class RetInst extends Instruction {


  public RetInst(Type type, int numOP) {
    super(TAG_.Ret, type, numOP);
  }
}
