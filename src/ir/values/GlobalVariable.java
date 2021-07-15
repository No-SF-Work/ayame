package ir.values;

import ir.types.ArrayType;
import ir.types.Type;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;

/**
 * 代表一个单独的全局量，全局Const常量必须被初始化
 */
public class GlobalVariable extends User {

  public GlobalVariable(String name, final Type type, Constant init) {
    super(name, type);
    module.__globalVariables.add(this);
    if (init != null) {
      this.COaddOperand(init);
    }
    this.init = init;
  }

  public void setConst() {
    isConst = true;
  }

  public boolean isConst = false;
  public Constant init;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getName()).append(" = dso_local ");
    if (isConst) {
      sb.append("constant ");
    } else {
      sb.append("global ");
    }
    if (this.getType().isIntegerTy()) {
      sb.append(this.getType().toString());
      sb.append(this.init == null ? "0 " : ((ConstantInt) this.init).getVal());
    } else if (this.getType().isArrayTy()) {
      sb.append(this.getType().toString());
      if (this.init == null) {
        sb.append("zeroinitializer ");
      } else {
        sb.append(ArrayType.buildConstInitStr((ArrayType) this.getType(), (ConstantArray) init));
      }
    }
    return sb.toString();
  }
}
