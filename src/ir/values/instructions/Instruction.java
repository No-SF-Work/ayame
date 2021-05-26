package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.User;
import util.Ilist;
//todo : code review

/**
 * Instruction可以作为链表元素
 */
public abstract class Instruction extends User implements Ilist<Instruction> {

  private static int HANDLE = 0;

  public enum TAG_ {
    //binary
    Add,
    Sub,
    Rsb,
    Mul,
    Div,
    Mod,
    Lt,
    Le,
    Ge,
    Gt,
    Eq,
    Ne,
    And,
    Or,
    //terminator
    Br,
    Ret,
    //mem op
    Alloca,
    Load,
    Store,
    GEP,
    Call,
    Phi;
  }

  /**
   * 在调用这些个指令的时候可能需要处理Use关系
   */
  public void insertBefore(Instruction next) {
    this.parent = next.parent;
    this.prev = next.prev;
    this.next = next;
    next.prev = this;
  }

  public void insertAfter(Instruction prev) {
    this.parent = prev.parent;
    this.prev = prev;
    this.next = prev.next;
    prev.next = this;
  }

  //将自己从所在的bb里和module里除去
  public void removeSelf() {
    if (this == this.parent.getEntry()) {
      this.getParent().setEntry(this.next);
      if (this.next != null) {
        this.next.prev = null;
      }
    }
    if (this == this.parent.getLast()) {
      this.parent.setLast(this.prev);
      if (this.prev != null) {
        this.prev.next = null;
      }
    }
    this.prev = null;
    this.next = null;
    module.__instructions.put(this.handle, null);
  }

  public Instruction getPrev() {
    return prev;
  }

  public Instruction getNext() {
    return next;
  }

  public void insertAtEnd(BasicBlock bb) {
    assert bb.getLast() != null;//bb 的 last 会在 set entry的时候更新
    this.parent = bb;
    bb.getLast().next = this;
    this.prev = bb.getLast();
    bb.setLast(this);
  }

  public Instruction(TAG_ tag, Type type, int numOP) {
    super("", type, numOP);
    this.tag = tag;
    this.handle = HANDLE;
    HANDLE++;
  }

  public boolean isBinary() {
    return this.tag.ordinal() <= TAG_.Or.ordinal()
        && this.tag.ordinal() >= TAG_.Add.ordinal();
  }

  public boolean isTerminator() {
    return this.tag.ordinal() >= TAG_.Br.ordinal()
        && this.tag.ordinal() <= TAG_.Ret.ordinal();
  }

  public boolean isMemOP() {
    return this.tag.ordinal() >= TAG_.Alloca.ordinal()
        && this.tag.ordinal() <= TAG_.Phi.ordinal();
  }


  public BasicBlock getParent() {
    return parent;
  }


  public void setParent(BasicBlock parent) {
    this.parent = parent;
  }


  public int getHandle() {
    return handle;
  }

  public static int getHANDLE() {
    return HANDLE;
  }

  public TAG_ getTag() {
    return tag;
  }

  public boolean isTerminator = false;
  protected BasicBlock parent = null;// 指令所属基本块
  protected Instruction prev = null;//前驱指令
  protected Instruction next = null;//后继指令
  public final TAG_ tag; //TAG作为分辨指令的凭据
  public final int handle;//fixme 生成一个全局唯一的表示符作为从module中的container里存取的依据
}