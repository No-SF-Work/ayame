package ir.values;

import ir.Use;
import ir.types.Type;
import ir.values.Constants.ConstantInt;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User对象，记录了自身操作的 Value 的状态
 * <p>
 * User 的 Operand 和 Use 类表达的是类似的东西，User表达的是use-def的角度，Value的 uses 表达的是， 只是为了处理方便同时做了两种设计，
 * <p>
 * ***以CO开头的方法保证了 Operand和Use(以及Use的operandRank)的正确以及一致性***
 * <p>
 * operand在list中的顺序也是有语义的，需要保持语义正确
 * <p>
 * 比如 想要创建一个 sub 的 BinaryInst
 * <p>
 * operand 0 是a，operand 1是b，显然将a和b颠倒会出现错误。
 * <p>
 */
public abstract class User extends Value {

  /**
   * 在你清楚有几个Operand的时候
   */
  public User(String name, Type type, int numOP) {
    super(name, type);
    this.numOP = numOP;
    this.operands = new ArrayList<>(
        Collections.nCopies(numOP, ConstantInt.CONST0()));
  }

  public User(String name, Type type) {
    super(name, type);
    this.operands = new ArrayList<>();
    this.numOP = 0;
  }

  /*
   * 下面是借鉴llvm的几个可能对优化比较方便的方法
   */

  /**
   * 添加一个operand
   */
  public void COaddOperand(Value v) {
    this.operands.add(v);
    v.addUse(new Use(v, this, numOP));
    this.numOP++;
  }

  /**
   * @param index:位置，operand视作从0开始
   * @param v:想要设置的operand
   **/
  public void CoSetOperand(int index, Value v) {
    assert this.numOP > index;
    this.operands.set(index, v);
    if (v != null) {
      v.addUse(new Use(v, this, index));
    }
  }

  /**
   * @param index: 位置，operand视作从0开始
   * @param v:     想要设置的operand
   *               <p>
   *               在 CoSetOperand 的基础上，维护 operand 的 usesList
   */
  public void CoReplaceOperandByIndex(int index, Value v) {
    var op = operands.get(index);
    this.CoSetOperand(index, v);
    if (op != null && !this.operands.contains(op)) {
      op.removeUseByUser(this);
    }
  }

  /**
   * remove一个或多个Operand，
   * <p>
   * 每次调用该函数都可能会导致operandlist的元素顺序变化，所以在需要按位置同时
   * <p>
   * 除去多个元素的时候应该一次性将所有位置传入
   */
  public void CORemoveNOperand(int[] a) {
    removeUsesOfOPs();
    this.numOP = 0;
    boolean in;
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
   * 将operandlist中所有lhs换为rhs，并更新Use fixme
   */
  public void COReplaceOperand(Value lhs, Value rhs) {

    //还没有实际写优化，暂时假定可能有一个lhs被用为复数次operand
    //update:确实有
    lhs.removeUseByUser(this);
    for (int i = 0; i < operands.size(); i++) {
      if (operands.get(i) == lhs) {
        CoSetOperand(i, rhs);
      }
    }
  }

  /**
   * 将自己的所有 operand 删除， 并删除所有相关的 Use
   */
  public void CORemoveAllOperand() {
    this.numOP = 0;
    removeUsesOfOPs();
    operands.clear();
  }

  /**
   * 将自己从所有Operand的Uselist中删除
   */
  public void removeUsesOfOPs() {
    if (operands == null) {
      return;
    }
    for (Value operand : operands) {
      if (operand != null) {
        operand.removeUseByUser(this);
      }
    }
  }

  public int getNumOP() {
    return numOP;
  }

  public void replaceValue(Value lhs, Value rhs) {
  }

  public ArrayList<Value> getOperands() {
    return operands;
  }

  protected ArrayList<Value> operands;
  protected int numOP;
}