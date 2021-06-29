package ir.values;

import ir.types.Type;

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

}
