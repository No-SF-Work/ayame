package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.User;

public abstract class Instruction extends User {
  private static int HANDLE = 0;
  public Instruction(String name, Type type, int numOP) {
    super(name, type, numOP);
    this.handle = HANDLE;
  }

  public enum OPcode {
    //todo 加上Opcode
  }

  BasicBlock parent = null;// 指令所属基本块
  Instruction prev = null;//前驱指令
  Instruction next = null;//后继指令

  /**
   * 将自己从Module，BasicBlock中抹去
   */
  public void killMe() {
    parent.instructions.remove(this);
    module._instructions.remove(this.handle);
  }


  public BasicBlock getParent() {
    return parent;
  }

  public Instruction getPrev() {
    return prev;
  }

  public Instruction getNext() {
    return next;
  }

  public void setParent(BasicBlock parent) {
    this.parent = parent;
  }

  public void setPrev(Instruction prev) {
    this.prev = prev;
  }

  public void setNext(Instruction next) {
    this.next = next;
  }

  public int getHandle() {
    return handle;
  }

  private int handle;//fixme 生成一个全局唯一的表示符作为从module中的container里存取的依据
}