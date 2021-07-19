package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;

public class UnaryInst extends Instruction {


  public UnaryInst(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public UnaryInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
    super(tag, type, numOP, parent);
  }

  public static class NotInst extends UnaryInst {

    public NotInst(TAG_ tag, Type type, int numOP) {
      super(TAG_.Not, type, numOP);
    }

    public NotInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(TAG_.Not, type, numOP, parent);
    }
  }

}
