package ir.values;

import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;

/**
 * 代表一个单独的全局量，全局Const常量必须被初始化
 */
public class GlobalVariable extends User {

  public GlobalVariable(String name, final Type type, Constant foldedInit, Constant plainInit) {
    super(name, new PointerType(type));
    module.__globalVariables.add(this);
    if (init != null) {
      this.COaddOperand(plainInit);
    }
    this.init = plainInit;
    this.fixedInit = foldedInit;
  }

  public void setConst() {
    isConst = true;
  }

  public boolean isConst = false;
  public Constant init; //todo 这个是展开成为一维数组的
  public Constant fixedInit;//todo 这个是封装成了ConstArray套ConstArray的形式的

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getName()).append(" = dso_local ");
    if (isConst) {
      sb.append("constant ");
    } else {
      sb.append("global ");
    }
    if (((PointerType) this.getType()).getContained().isIntegerTy()) {
      sb.append(((PointerType) this.getType()).getContained().toString()).append(" ");
      sb.append(this.init == null ? "0 " : ((ConstantInt) this.init).getVal());
    } else if (((PointerType) this.getType()).getContained().isArrayTy()) {
      if (this.fixedInit == null) {
        sb.append(((PointerType) this.getType()).getContained().toString()).append(" ");
        sb.append("zeroinitializer ");
      } else {
        sb.append(fixedInit.toString());
      }
    }
    return sb.toString();
  }
}
