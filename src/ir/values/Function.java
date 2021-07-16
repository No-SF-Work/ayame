package ir.values;

import ir.Analysis.LoopInfo;
import ir.MyModule;
import ir.types.FunctionType;
import ir.types.Type;

import java.util.ArrayList;
import java.util.List;
import util.IList;
import util.IList.INode;

/**
 * Function类,代表一个函数，拥有一串基本块，一串参数，一个SymbolTable //
 */
public class Function extends Value {

  //参数声明，不含值
  public class Arg extends Value {

    private int rank;//排第几,非负

    public int rank() {
      return this.rank;
    }

    public Arg(Type type, int rank) {
      super("", type);
      this.rank = rank;
    }

    public List<Value> getBounds() {
      return bounds;
    }

    public void setBounds(List<Value> bounds) {
      this.bounds = bounds;
    }

    private List<Value> bounds;

  }

  private void buildArgs() {
    var funcTy = this.getType();
    var arr = funcTy.getParams();
    for (int i = 0; i < funcTy.getParams().size(); i++) {
      argList_.add(new Arg(arr.get(i), i));
    }
  }


  public Function(String name, Type type) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    argList_ = new ArrayList<>();
    buildArgs();
  }

  public Function(String name, Type type, MyModule module) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
    argList_ = new ArrayList<>();
    buildArgs();
  }

  public Function(String name, Type type, MyModule module, boolean isBuiltin) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
    argList_ = new ArrayList<>();
    buildArgs();
    this.isBuiltin_ = isBuiltin;
  }

  public ArrayList<Arg> getArgList() {
    return argList_;
  }

  public int getNumArgs() {
    return argList_.size();
  }

  public LoopInfo getLoopInfo() {
    return loopInfo;
  }

  public IList<BasicBlock, Function> getList_() {
    return list_;
  }

  @Override
  public FunctionType getType() {
    return (FunctionType) super.getType();
  }

  public boolean isBuiltin_() {
    return isBuiltin_;
  }

  private boolean isBuiltin_ = false;//lib function
  private IList<BasicBlock, Function> list_;
  private INode<Function, MyModule> node;
  private ArrayList<Arg> argList_;//有序参数列表
  private LoopInfo loopInfo; // 函数内的循环信息

  public boolean isHasSideEffect() {
    return hasSideEffect;
  }

  public void setHasSideEffect(boolean hasSideEffect) {
    this.hasSideEffect = hasSideEffect;
  }

  public boolean isUsedGlobalVariable() {
    return usedGlobalVariable;
  }

  public void setUsedGlobalVariable(boolean usedGlobalVariable) {
    this.usedGlobalVariable = usedGlobalVariable;
  }

  private boolean hasSideEffect = false;
  private boolean usedGlobalVariable = false;
}
