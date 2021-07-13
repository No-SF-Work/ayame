package ir.values.instructions;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.User;
import util.IList.INode;
//todo : code review

/**
 * Instruction可以作为链表元素
 */
public abstract class Instruction extends User {

  private static int HANDLE = 0;

  public enum TAG_ {
    //binary
    Add,
    Sub,
    Rsb,
    Mul,
    Div,
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
    Call,
    Ret,
    //mem op
    Alloca,
    Load,
    Store,
    GEP,
    Phi,
    MemPhi,
    Zext,
  }

  public Instruction(TAG_ tag, Type type, int numOP) {
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
  }

  public Instruction(TAG_ tag, Type type, int numOP, BasicBlock parent) {/** Insert at bb end*/
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
    this.node.insertAtEnd(parent.getList());
  }

  public Instruction(TAG_ tag, Type type, int numOP, Instruction prev) {/** Insert self after Inst*/
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
    this.node.insertAfter(prev.node);
  }

  public Instruction(Instruction next, TAG_ tag, Type type,
      int numOP) {/** Insert self before Inst*/
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
    this.node.insertBefore(next.node);
  }

  public boolean isBinary() {
    return this.tag.ordinal() <= TAG_.Or.ordinal()
        && this.tag.ordinal() >= TAG_.Add.ordinal();
  }

  public boolean isArithmeticBinary() {
    return this.tag.ordinal() >= TAG_.Add.ordinal()
        && this.tag.ordinal() <= TAG_.Div.ordinal();
  }

  public boolean isLogicalBinary() {
    return this.tag.ordinal() >= TAG_.Lt.ordinal()
        && this.tag.ordinal() <= TAG_.Or.ordinal();
  }

  public boolean isTerminator() {
    return this.tag.ordinal() >= TAG_.Br.ordinal()
        && this.tag.ordinal() <= TAG_.Ret.ordinal();
  }

  public boolean isMemOP() {
    return this.tag.ordinal() >= TAG_.Alloca.ordinal()
        && this.tag.ordinal() <= TAG_.MemPhi.ordinal();
  }

  public static int getHANDLE() {
    return HANDLE;
  }

  public BasicBlock getBB() {
    return this.node.getParent().getVal();
  }

  public INode<Instruction, BasicBlock> node;//(BasicBlock)parent =node.getparent.getval;
  public final TAG_ tag; //TAG作为分辨指令的凭据
  public final int handle;//fixme 生成一个全局唯一的表示符作为从module中的container里存取的依据
}