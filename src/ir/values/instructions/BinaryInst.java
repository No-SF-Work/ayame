package ir.values.instructions;

import ir.types.IntegerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constants.ConstantInt;
import ir.values.Value;

public class BinaryInst extends Instruction {

  //不插
  public BinaryInst(TAG_ tag, Type type, Value lhs, Value rhs) {
    super(tag, type, 2);
    if (this.isLogicalBinary()) {
      setType(IntegerType.getI1());
    }
    if (this.isArithmeticBinary()) {
      setType(IntegerType.getI32());
    }
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在bb末尾
  public BinaryInst(TAG_ tag, Type type, Value lhs, Value rhs, BasicBlock parent) {
    super(tag, type, 2, parent);
    if (this.isLogicalBinary()) {
      setType(IntegerType.getI1());
    }
    if (this.isArithmeticBinary()) {
      setType(IntegerType.getI32());
    }
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在next前面
  public BinaryInst(Instruction next, TAG_ tag, Type type, Value lhs, Value rhs) {
    super(next, tag, type, 2);
    if (this.isLogicalBinary()) {
      setType(IntegerType.getI1());
    }
    if (this.isArithmeticBinary()) {
      setType(IntegerType.getI32());
    }
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在prev后面
  public BinaryInst(TAG_ tag, Type type, Value lhs, Value rhs, Instruction prev) {
    super(tag, type, 2, prev);
    if (this.isLogicalBinary()) {
      setType(IntegerType.getI1());
    }
    if (this.isArithmeticBinary()) {
      setType(IntegerType.getI32());
    }
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  // 假设调用时已经知道 lhs 和 rhs 都是 ConstantInt 了
  public static int evalBinary(TAG_ tag, ConstantInt clhs, ConstantInt crhs) {
    Integer lhsVal = clhs.getVal();
    Integer rhsVal = crhs.getVal();
    switch (tag) {
      case Add:
        return lhsVal + rhsVal;
      case Sub:
        return lhsVal - rhsVal;
      case Rsb:
        return rhsVal - lhsVal;
      case Mod:
        return lhsVal % rhsVal;
      case Mul:
        return lhsVal * rhsVal;
      case Div:
        assert rhsVal != 0;
        return lhsVal / rhsVal;
      case Lt:
        return (lhsVal < rhsVal) ? 1 : 0;
      case Le:
        return (lhsVal <= rhsVal) ? 1 : 0;
      case Ge:
        return (lhsVal >= rhsVal) ? 1 : 0;
      case Gt:
        return (lhsVal > rhsVal) ? 1 : 0;
      case Eq:
        return (lhsVal.equals(rhsVal)) ? 1 : 0;
      case Ne:
        return (!lhsVal.equals(rhsVal)) ? 1 : 0;
      case And:
        return (lhsVal != 0 && rhsVal != 0) ? 1 : 0;
      case Or:
        return (lhsVal != 0 || rhsVal != 0) ? 1 : 0;
    }
    return 0;
  }

  public boolean isAdd() {
    return this.tag == TAG_.Add;
  }

  public boolean isSub() {
    return this.tag == TAG_.Sub;
  }

  public boolean isRsb() {
    return this.tag == TAG_.Rsb;
  }

  public boolean isMul() {
    return this.tag == TAG_.Mul;
  }

  public boolean isDiv() {
    return this.tag == TAG_.Div;
  }

  public boolean isMod() {
    return this.tag == TAG_.Mod;
  }


  public boolean isCommutative() {
    return this.tag == TAG_.Add || this.tag == TAG_.Mul || this.tag == TAG_.Eq
        || this.tag == TAG_.Ne || this.tag == TAG_.And || this.tag == TAG_.Or;
  }

  public static boolean isRev(TAG_ a, TAG_ b) {
    return (a == TAG_.Lt && b == TAG_.Gt) || (a == TAG_.Gt && b == TAG_.Lt) || (a == TAG_.Le
        && b == TAG_.Ge) || (a == TAG_.Ge && b == TAG_.Le);
  }

  public boolean isLt() {
    return this.tag == TAG_.Lt;
  }

  public boolean isLe() {
    return this.tag == TAG_.Le;
  }

  public boolean isGe() {
    return this.tag == TAG_.Ge;
  }

  public boolean isGt() {
    return this.tag == TAG_.Gt;
  }

  public boolean isEq() {
    return this.tag == TAG_.Eq;
  }

  public boolean isNe() {
    return this.tag == TAG_.Ne;
  }

  public boolean isAnd() {
    return this.tag == TAG_.And;
  }

  public boolean isOr() {
    return this.tag == TAG_.Or;
  }

  public boolean isCond() {
    return this.tag == TAG_.Lt || this.tag == TAG_.Le || this.tag == TAG_.Gt ||
        this.tag == TAG_.Ge || this.tag == TAG_.Eq || this.tag == TAG_.Ne;
  }

  public String secondName;
  public String thirdName;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getName() + " = ");
    sb.append(
        switch (this.tag) {
          case Add -> "add  i32 ";
          case Sub -> "sub  i32 ";
          case Rsb -> "rsb  i32 ";
          case Shl -> "shl i32 ";
          case Shr -> "ashr i32 ";
          case Mul -> "mul  i32 ";
          case Div -> "sdiv  i32 ";
          case Lt -> "icmp slt " + this.getOperands().get(0).getType() + " ";
          case Le -> "icmp sle " + this.getOperands().get(0).getType() + " ";
          case Ge -> "icmp sge " + this.getOperands().get(0).getType() + " ";
          case Gt -> "icmp sgt " + this.getOperands().get(0).getType() + " ";
          case Eq -> "icmp eq " + this.getOperands().get(0).getType() + " ";
          case Ne -> "icmp ne  " + this.getOperands().get(0).getType() + " ";
          case And -> "";
          case Or -> "";//todo and and or
          default -> "";
        }
    );
    sb.append(operands.get(0).getName() + "," + operands.get(1).getName());
    return sb.toString();
  }

  public Value selfAlgebraOpt() {
    switch (this.tag) {
      case Add: {

      }
    }
    return null;
  }
}
