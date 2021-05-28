package ir.values;

import ir.types.FunctionType;
import ir.types.Type;

import java.util.ArrayList;
import util.IList;

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
    assert type instanceof FunctionType;
    list_ = new IList<>(this);
  }

  public int getNumArgs() {
    return argList_.size();
  }

  private boolean isBuiltin_ = false;//lib function
  private IList<BasicBlock, Function> list_;
  private ArrayList<Arg> argList_;//有序参数列表
}
