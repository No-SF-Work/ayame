package ir.values.instructions;

import ir.types.FunctionType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import java.util.ArrayList;

public abstract class TerminatorInst extends Instruction {

  public TerminatorInst(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public TerminatorInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {//这些指令只会出现在bb的结尾
    super(tag, type, numOP, parent);
  }

  public TerminatorInst(TAG_ tag, Type type, int numOP, Instruction prev) {
    super(tag, type, numOP, prev);
  }

  public TerminatorInst(Instruction next, TAG_ tag, Type type, int numOP) {
    super(next, tag, type, numOP);
  }

  public static class CallInst extends TerminatorInst {

    /**
     * 调用Func函数，args是传入的参数，bb是想要放置的bb
     */
    public CallInst(Function func, ArrayList<Value> args, BasicBlock bb) {
      super(TAG_.Call, ((FunctionType) func.getType()).getRetType(), args.size() + 1, bb);
      assert func.getNumArgs() == args.size();
      if (this.getType().isVoidTy()) {
        needname = false;
      }
      CoSetOperand(0, func);//op1 is func
      for (int i = 0; i < args.size(); i++) {
        CoSetOperand(i + 1, args.get(i));//args
      }
    }

    public CallInst(Function func, ArrayList<Value> args) {
      super(TAG_.Call, func.getType().getRetType(), args.size() + 1);
      assert func.getNumArgs() == args.size();
      if (this.getType().isVoidTy()) {
        needname = false;
      }
      CoSetOperand(0, func);//op1 is func
      for (int i = 0; i < args.size(); i++) {
        CoSetOperand(i + 1, args.get(i));//args
      }
    }

    public boolean isPureCall() {
      Function func = (Function) this.getOperands().get(0);
      if (func.isHasSideEffect() || func.isUsedGlobalVariable()) {
        return false;
      }
      for (Value val : this.getOperands()) {
//        if (ArrayAliasAnalysis.getArrayValue(val) != null) {
        if (val instanceof GEPInst || (val instanceof LoadInst && !val.getType().isI32()
            && !((PointerType) val.getType()).getContained().isI32())) {
          return false;
        }
      }
      return true;
    }

    public Function getFunc() {
      return (Function) this.getOperands().get(0);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (((FunctionType) this.operands.get(0).getType()).getRetType().isVoidTy()) {
        sb.append("call ").append(this.getType()).append(" @").append(operands.get(0).getName());
      } else {
        sb.append(this.getName()).append(" = call ").append(this.getType()).append(" @")
            .append(operands.get(0).getName());
      }

      sb.append("(");
      boolean a = false;
      for (int i = 1; i < operands.size(); i++) {
        a = true;
        sb.append(operands.get(i).getType()).append(" ").append(operands.get(i).getName())
            .append(",");
      }
      if (a) {
        sb.deleteCharAt(sb.length() - 1);
      }
      sb.append(")");
      return sb.toString();
    }

    public boolean hasAlias;
  }

  public static class BrInst extends TerminatorInst {

    /**
     * 条件转移
     */
    enum prefer {
      T,//prefer true
      F,//prefer false
      D //default
    }

    prefer pre = prefer.D;

    public prefer getPrefer() {
      return pre;
    }

    public void setPrefer(prefer pre) {
      this.pre = pre;
    }

    public BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 3, parent);
      this.CoSetOperand(
          0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
      needname = false;
    }

    public BrInst(Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
      super(TAG_.Br, VoidType.getType(), 3);
      this.CoSetOperand(0, cond);
      this.CoSetOperand(1, trueBlock);
      this.CoSetOperand(2, falseBlock);
      needname = false;
    }

    /**
     * 无条件转移
     */
    public BrInst(BasicBlock trueBlock, BasicBlock parent) {
      super(TAG_.Br, Type.VoidType.getType(), 1, parent);
      needname = false;
      this.CoSetOperand(0, trueBlock);
    }

    public BrInst(BasicBlock trueblock) {
      super(TAG_.Br, Type.VoidType.getType(), 1);
      this.CoSetOperand(0, trueblock);
      needname = false;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("br ");
      if (this.numOP == 1) {
        sb.append(operands.get(0).getType()).append(" ").append("%" + operands.get(0).getName());
      }
      if (this.numOP == 3) {
        sb.append(operands.get(0).getType()).append(" ").append(operands.get(0).getName())
            .append(",");
        sb.append(operands.get(1).getType()).append(" ").append("%" + operands.get(1).getName())
            .append(",");
        sb.append(operands.get(2).getType()).append(" ").append("%" + operands.get(2).getName())
            .append(" ");
      }
      sb.append("\n");
      return sb.toString();
    }
  }

  public static class RetInst extends TerminatorInst {

    /**
     * ret i32 插在bb末尾
     */
    public RetInst(Value val, BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 1, parent);
      this.CoSetOperand(0, val);
      needname = false;
    }

    public RetInst(Value val) {
      super(TAG_.Ret, VoidType.getType(), 1);
      this.CoSetOperand(0, val);
      needname = false;
    }

    /**
     * ret void 插在bb末尾
     */
    public RetInst(BasicBlock parent) {
      super(TAG_.Ret, VoidType.getType(), 0, parent);
      needname = false;
    }

    public RetInst() {
      super(TAG_.Ret, VoidType.getType(), 0);
      needname = false;
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ret ");
      if (this.numOP == 1) {
        sb.append(operands.get(0).getType() + " " + operands.get(0).getName());
      } else {
        sb.append("void ");
      }
      return sb.toString();
    }
  }

  public boolean isRet() {
    return this.tag == TAG_.Ret;
  }

  public boolean isBr() {
    return this.tag == TAG_.Br;
  }
}
