package ir;

import ir.values.User;
import ir.values.Value;
import java.util.Objects;

/**
 * 用来记录Value之间的使用关系，一个Value使用了另一个Value，就有一条有向边连接他们          _- E Use和作为User的Value一个用来找def-use，一个用来找use-def
 */
public class Use {

  public Use(Value v, User u) {
    this.v = v;
    this.u = u;
    this.operandRank = u.getNumOP();
  }

  public Use(Value v, User u, int operandRank) {
    this.v = v;
    this.u = u;
    this.operandRank = operandRank;
  }

  public void setValue(Value v) {
    this.v = v;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Use use = (Use) o;
    return operandRank == use.operandRank && Objects.equals(u, use.u) && Objects
        .equals(v, use.v);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operandRank, u, v);
  }

  public User getUser() {
    return this.u;
  }

  public int getOperandRank() {
    return operandRank;
  }

  private int operandRank;
  private User u;
  private Value v;
}
