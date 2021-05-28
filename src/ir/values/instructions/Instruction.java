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

  public Instruction(TAG_ tag, Type type, int numOP, Instruction prev) {/** Insert after Inst*/
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
    this.node.insertAfter(prev.node);
  }

  public Instruction(Instruction next, TAG_ tag, Type type, int numOP) {/** Insert before Inst*/
    super("", type, numOP);
    this.node = new INode<>(this);
    this.tag = tag;
    this.handle = HANDLE;
    module.__instructions.put(this.handle, this);
    HANDLE++;
    next.node.insertBefore(this.node);
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
  public static int getHANDLE() {
    return HANDLE;
  }

  public INode<Instruction, BasicBlock> node;//(BasicBlock)parent =node.getparent.getval;
  public final TAG_ tag; //TAG作为分辨指令的凭据
  public final int handle;//fixme 生成一个全局唯一的表示符作为从module中的container里存取的依据
}