package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Value;
import util.Ilist;

public class TerminatorInst {

  public static class BrInst extends Instruction {

    /**
     * 条件转移
     */
    BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock cur) {
      super(TAG_.Br, Type.VoidType.getType(), 3);
      this.CoSetOperand(
          0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
      this.isTerminator = true;//todo
    }

    /**
     * 无条件转移
     */
    public BrInst() {
      super(TAG_.Br, Type.VoidType.getType(), 3);
    }

    BasicBlock T;
    BasicBlock F;
  }

  public static class RetInst extends Instruction {

    //todo
    public RetInst(TAG_ tag, Type type, int numOP) {
      super(tag, type, numOP);
    }
  }
}
