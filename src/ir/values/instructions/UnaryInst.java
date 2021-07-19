package ir.values.instructions;

import ir.types.IntegerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Value;

public class UnaryInst extends Instruction {


  //llvm 里没有 Not Inst , 是用icmp ne 0 和 xor 进行的转换
  //在这里的name2是为了导出llvm ir 时保证语法正确，用来debug
  private String name2;

  public void setName2(String name2) {
    this.name2 = name2;
  }

  public UnaryInst(TAG_ tag, Type type, int numOP) {
    super(tag, type, numOP);
  }

  public UnaryInst(TAG_ tag, Type type, int numOP, BasicBlock parent) {
    super(tag, type, numOP, parent);
  }

  public static class NotInst extends UnaryInst {

    public NotInst(Value source) {
      super(TAG_.Not, IntegerType.getI1(), 1);
    }

    public NotInst(Value source, BasicBlock parent) {
      super(TAG_.Not, IntegerType.getI1(), 1, parent);
      this.CoSetOperand(0, source);
    }
  }

  @Override
  public String toString() {
    // TODO: 2021/7/19
    return null;
  }
}
