package ir.values.instructions;

import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Value;
import util.Ilist;

public abstract class TerminatorInst extends Instruction {

  public TerminatorInst(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public TerminatorInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {//这些指令只会出现在bb的结尾
    super(tag, type, numOP, parent);
  }


  public static class BrInst extends TerminatorInst {

    /**
     * 条件转移 不插
     */
    BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
      super(TAG_.Br, Type.VoidType.getType(), 3);
      this.CoSetOperand(
          0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
      this.isTerminator = true;
    }

    /**
     * 条件转移 插在bb末尾
     */
    public BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 3, parent);
      this.CoSetOperand(
          0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
      this.isTerminator = true;
    }

    /**
     * 无条件转移 不插
     */
    public BrInst(BasicBlock trueBlock) {
      super(TAG_.Br, Type.VoidType.getType(), 1);
      this.CoSetOperand(0, trueBlock);
    }

    /**
     * 无条件转移 插在bb末尾
     */
    public BrInst(BasicBlock trueBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 1, parent);
      this.CoSetOperand(0, trueBlock);
    }
  }

  public static class RetInst extends TerminatorInst {

    /**
     * ret I32 不插
     */
    public RetInst(Value val) {
      super(TAG_.Ret, VoidType.getType(), 1);
      this.CoSetOperand(0, val);
    }

    /**
     * ret i32 插在bb末尾
     */
    public RetInst(Value val, BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 1, parent);
      this.CoSetOperand(0, val);
    }

    /**
     * ret void 不插
     */
    public RetInst() {
      super(TAG_.Ret, VoidType.getType(), 0);
    }

    /**
     * ret void 插在bb末尾
     */
    public RetInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 0, parent);
    }
  }

  public boolean isRet() {
    return this.tag == TAG_.Ret;
  }

  public boolean isBr() {
    return this.tag == TAG_.Br;
  }
}
