package ir.values;

import ir.types.Type;

/**
 * 代表一个单独的全局量，是指向从compiler中申请到的空间的常量指针， 可以被初始化，全局常量必须被初始化
 */
public class GlobalVariable extends User {


  private boolean isConstant;
  private Constant initVal;

  public GlobalVariable(String name, Type type, int numOP) {
    super(name, type, numOP);
  }
}
