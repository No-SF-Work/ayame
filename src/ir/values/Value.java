package ir.values;

import ir.types.Type;
import ir.Use;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * 借鉴的llvm ir 的设计方式，基本上所有变量/常量/表达式/符号都是 Value
 * <p>
 * 原因详情可见 https://www.cnblogs.com/Five100Miles/p/14083814.html
 * <p>
 * ***Use(以及Use的paramNum) 和 operand应该保持一致***
 * <p>
 * ***以CO开头的修改DU关系的方法是同步更新的*** fixme
 */
public abstract class Value {

  /**
   * 每个Value应该有一个独一无二的name
   */
  public Value(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }


  /**
   * 将所有对 this 的使用换为对v的使用，连带着更新User的Operand
   */
  public void COReplaceAllUseWith(Value v) {
    User tmp;
    for (Use use : usesList) {
      use.getUser().COReplaceOperand(this, v);
    }
  }

  /**
   * 删去自身的一条Use，连带着修改User的operands
   */
  public void CORemoveUse(User usr) {
    usesList.removeIf(use -> use.getUser() == (usr));
    usr.removeOperand(this);
  }

  protected void removeUseByUser(User usr) {
    usesList.removeIf(use -> use.getUser() == (usr));
  }

  public void removeUse(Use u) {
    this.usesList.remove(u);
  }

  public void addUse(Use u) {
    this.usesList.add(u);
  }

  public Type getType() {
    return type;
  }//所有的Value都指明了一个Type的

  private Value parent;
  private LinkedList<Use> usesList;//记录使用这个Value的所有User
  private String name;
  private final Type type;
}
