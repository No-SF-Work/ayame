package ir.values;

import ir.types.Type;

import java.util.ArrayList;

/**
 * Function类,代表一个函数，拥有一串基本块，一串参数，一个SymbolTable //todo 考虑函数内联
 */
public class Function extends User {

  //参数声明，不含值
  public class Arg extends Value {
    //todo 做对函数内联方便的架构设计


    private int rank;//排第几,非负

    public Arg(String name, Type type) {
      super(name, type);
    }
  }

  public Function(String name, Type type, int numOP) {
    super(name, type, numOP);
  }

  private boolean isBuiltin;//lib function
  private ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
  private ArrayList<Arg> argList;//有序参数列表
}
