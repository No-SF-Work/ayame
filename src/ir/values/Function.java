package ir.values;

import ir.MyModule;
import ir.types.FunctionType;
import ir.types.Type;

import java.util.ArrayList;
import util.IList;
import util.IList.INode;

/**
 * Function类,代表一个函数，拥有一串基本块，一串参数，一个SymbolTable //
 */
public class Function extends Value {

  //参数声明，不含值
  public class Arg extends Value {

    //todo 做对函数内联方便的架构设计
    private int rank;//排第几,非负

    public Arg(Type type, int rank) {
      super("", type);
      this.rank = rank;
    }
  }

  public Function(String name, Type type) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
  }

  public Function(String name, Type type, MyModule module) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
  }
  
  public Function(String name, Type type, MyModule module, boolean isBuiltin) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
    this.isBuiltin_ = isBuiltin;
  }

  public int getNumArgs() {
    return argList_.size();
  }

  public IList<BasicBlock, Function> getList_() {
    return list_;
  }

  private boolean isBuiltin_ = false;//lib function
  private IList<BasicBlock, Function> list_;
  private INode<Function, MyModule> node;
  private ArrayList<Arg> argList_;//有序参数列表
}
