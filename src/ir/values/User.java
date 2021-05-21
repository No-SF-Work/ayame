package ir.values;

import ir.Use;
import ir.types.Type;
import java.awt.List;
import java.util.ArrayList;

/**
 * User对象，记录了自身操作的 Value 的状态
 * <p>
 * User 的 Operand 和 Use 类表达的是类似的东西，User表达的是use-def的角度，Value的 uses 表达的是， 只是为了处理方便同时做了两种设计，
 * <p>
 * ***以CO开头的方法保证了 Operand和Use(以及Use的operandRank)的正确以及一致性***
 * <p>
 * fixme :把operand的关系理清楚
 */
public abstract class User extends Value {

  public User(String name, Type type, int numOP) {
    super(name, type);
    this.numOP = numOP;
    this.operands = new ArrayList<>(numOP);
  }


  /**
   * 下面是借鉴llvm的几个可能对优化比较方便的方法
   */

  public void COaddOperand(Value v) {
    this.operands.add(v);
    v.addUse(new Use(v, this));
    this.numOP++;
  }

  /**
   * @param index:operand视作从0开始
   **/
  public void CoSetOperand(int index, Value v) {
    this.operands.set(index, v);
    v.addUse(new Use(v, this, index));
  }

  /**
   * remove一个或多个Operand，
   * <p>
   * 每次调用该函数都可能会导致list的元素顺序变化，所以在需要按位置同时
   * <p>
   * 除去多个元素的时候应该一次性将所有位置传入
   */
  public void CORemoveNOperand(int[] a) {
    removeUsesOfOPs();
    this.numOP = 0;
    boolean in = false;
    ArrayList<Value> tmp = this.operands;
    this.operands = new ArrayList<>();
    for (int i = 0; i < tmp.size(); i++) {
      in = false;
      for (int k : a) {
        //看现在这个Value的索引在不在a的数组里面
        if (i == k) {
          in = true;
          break;
        }
      }
      //不在就把这个Value加回operands里
      if (!in) {
        this.COaddOperand(tmp.get(i));
      }
    }
  }

  /**
   * 将operandlist中所有lhs换为rhs，连带着更新Use fixme
   */
  public void COReplaceOperand(Value lhs, Value rhs) {
    assert this.operands.contains(lhs);
    //
    //还没有实际写优化，暂时假定可能有一个lhs被用为复数次operand todo
    for (int i = 0; i < operands.size(); i++) {
      if (operands.get(i) == lhs) {
        operands.set(i, rhs);
      }
    }
  }

  /**
   * 将自己的所有Operand删除， 连带删除自己的所有Operand关于自己的Use
   */
  public void CORemoveAllOperand() {
    removeUsesOfOPs();
    operands.clear();
  }

  /**
   * 将自己从所有Operand的Uselist中删除
   */
  protected void removeUsesOfOPs() {
    for (Value operand : operands) {
      operand.removeUseByUser(this);
    }
    this.operands.clear();
  }

  public void removeOperand(Value v) {
    this.operands.removeIf(op -> op == v);
  }

  public Value getOperand(int i) {
    return operands.get(i);
  }

  public int getNumOP() {
    return numOP;
  }

  private ArrayList<Value> operands;
  private int numOP;
}
//todo