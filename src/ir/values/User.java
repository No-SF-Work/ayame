package ir.values;

import ir.Use;
import ir.types.Type;
import java.util.ArrayList;

/**
 * User对象，记录了自身操作的 Value 的状态 要记住的一点是 User 的 Operand 和 Use 类表达的是同一个东西， 只是为了处理方便同时做了两种设计，需要保持 Operand
 * 和 Use 的一致
 */
public abstract class User extends Value {

  public User(String name, Type type, int numOP) {
    super(name, type);
    this.numOP = numOP;
  }

  public Value getOperand(int i) {
    return operands.get(i);
  }

  /**
   * 将自己从所有Operand的Uselist中删除
   */
  public void removeUsesOfOPs() {
    for (Value operand : operands) {
      //todo 加方法
    }
  }

  private ArrayList<Value> operands;
  private int numOP;
}
//todo