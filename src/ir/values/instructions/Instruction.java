package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.User;


public abstract class Instruction extends User {

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

  private static int HANDLE = 0;

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


  /**
   * 将自己从Module，BasicBlock中抹去
   */
  public void killMe() {
    parent.getInstructions().remove(this);
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
    prev.next = this;
  }

  /**
   * 双向绑定
   */
  public void setNext(Instruction next) {
    this.next = next;
    next.prev = this;
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

  protected BasicBlock parent = null;// 指令所属基本块
  protected Instruction prev = null;//前驱指令
  protected Instruction next = null;//后继指令
  public final TAG_ tag; //TAG作为分辨指令的凭据
  private final int handle;//fixme 生成一个全局唯一的表示符作为从module中的container里存取的依据
}