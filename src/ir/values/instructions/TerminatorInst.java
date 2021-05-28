package ir.values.instructions;

import ir.types.FunctionType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import java.util.ArrayList;

public abstract class TerminatorInst extends Instruction {

  public TerminatorInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {//这些指令只会出现在bb的结尾
    super(tag, type, numOP, parent);
  }

  public static class CallInst extends TerminatorInst {

    /**
     * 给 builtin func 用的
     **/
    public CallInst(Function func, BasicBlock parent) {
      super(TAG_.Call, ((FunctionType) func.getType()).getRetType(), func.getNumArgs(), parent);
    }

    /**
     * 调用Func函数，args是传入的参数，bb是想要放置的bb
     */
    public CallInst(Function func, ArrayList<Value> args, BasicBlock bb) {
      super(TAG_.Call, ((FunctionType) func.getType()).getRetType(), args.size() + 1, bb);
      assert func.getNumArgs() == args.size();
      CoSetOperand(0, func);//op1 is func
      for (int i = 0; i < args.size(); i++) {
        CoSetOperand(i + 1, args.get(i));//args
      }
    }
  }

  public static class BrInst extends TerminatorInst {

    /**
     * 条件转移
     */
    public BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 3, parent);
      this.CoSetOperand(
          0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
    }

    /**
     * 无条件转移
     */
    public BrInst(BasicBlock trueBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 1, parent);
      this.CoSetOperand(0, trueBlock);
    }

    @Override
    public String toString() {
      //todo
      return super.toString();
    }
  }

  public static class RetInst extends TerminatorInst {

    /**
     * ret i32 插在bb末尾
     */
    public RetInst(Value val, BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 1, parent);
      this.CoSetOperand(0, val);
    }

    /**
     * ret void 插在bb末尾
     */
    public RetInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 0, parent);
    }

    @Override
    public String toString() {
      //todo
      return super.toString();
    }
  }

  public boolean isRet() {
    return this.tag == TAG_.Ret;
  }

  public boolean isBr() {
    return this.tag == TAG_.Br;
  }
}
