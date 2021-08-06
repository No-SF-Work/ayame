package ir.values;

import ir.MyModule;
import ir.Use;
import ir.types.Type;
import ir.values.instructions.Instruction;
import java.util.LinkedList;

/**
 * 借鉴的llvm ir 的设计方式，基本上所有变量/常量/表达式/符号都是 Value
 * <p>
 * 原因详情可见 https://www.cnblogs.com/Five100Miles/p/14083814.html
 * <p>
 * ***Use(以及Use的paramNum) 和 operand应该保持一致***
 * <p>
 */
public abstract class Value {

  //module is approachable for all value
  //module 对所有Value暴露，因为我们只需要考虑单文件编译的情况,这很难看，但是方便
  public static MyModule module = MyModule.getInstance();

  /**
   * 每个Value应该有一个独一无二的name以及一个type
   */
  public Value(String name, Type type) {
    this.usesList = new LinkedList<>();
    this.name = name;
    this.type = type;
  }

  public Value(Type type) {
    this.type = type;
    this.name = "";
    this.usesList = new LinkedList<>();
    //todo this.name = auto generated name;
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
  public void
  COReplaceAllUseWith(Value v) {
    User usr;
    for (Use use : usesList) {
      use.getUser().CoSetOperand(use.getOperandRank(), v);
    }
    this.usesList.clear();
  }

  /**
   * 删去自身的一条Use，并将User的operand设置为null
   */
  public void CORemoveUse(Use use) {
    use.getUser().getOperands().set(use.getOperandRank(), null);
    usesList.remove(use);
  }

  protected void removeUseByIndexAndUser(User usr, int index) {
    usesList.removeIf(use -> use.getUser() == usr && use.getOperandRank() == index);
  }

  public void removeUseByUser(User usr) {
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
  }//所有的Value都需要指明一个Type

  public void setType(Type type_) {
    this.type = type_;
  }

  public LinkedList<Use> getUsesList() {
    return usesList;
  }

  public boolean isFunction() {
    return this instanceof Function;
  }

  public boolean isBasicBlock() {
    return this instanceof BasicBlock;
  }

  public boolean isInstruction() {
    return this instanceof Instruction;
  }

  private LinkedList<Use> usesList;//记录使用这个Value的所有User
  private String name;
  private Type type;
}
