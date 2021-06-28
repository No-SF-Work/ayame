package ir.values;

import ir.types.Type;

/**
 * 代表一个单独的全局量，全局Const常量必须被初始化
 */
public class GlobalVariable extends User {

  public GlobalVariable(String name, final Type type, int numOP, Constant init) {
    super(name, type, numOP);
    this.init = init;
  }

  public void setConstant() {
    isConstant = true;
  }

  public boolean isConstant = false;
  public Constant init;

}
