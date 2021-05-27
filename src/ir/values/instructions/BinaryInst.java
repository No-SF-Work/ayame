package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Value;

public abstract class BinaryInst extends Instruction {

  //不插
  public BinaryInst(TAG_ tag, Type type, Value lhs, Value rhs) {
    super(tag, type, 2);
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在bb末尾
  public BinaryInst(TAG_ tag, Type type, BasicBlock parent, Value lhs, Value rhs) {
    super(tag, type, 2, parent);
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在next前面
  public BinaryInst(Instruction next, TAG_ tag, Type type, Value lhs, Value rhs) {
    super(next, tag, type, 2);
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
  }

  //插在prev后面
  public BinaryInst(TAG_ tag, Type type, Instruction prev, Value lhs, Value rhs) {
    super(tag, type, 2, prev);
    this.CoSetOperand(0, lhs);
    this.CoSetOperand(1, rhs);
    module.__instructions.put(this.handle, this);
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

  @Override
  public String toString() {
    switch (this.tag) {
      case Alloca -> {
      }
      default -> {
      }
    }
    //todo
    return null;
  }

}
