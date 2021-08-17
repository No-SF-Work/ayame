package ir.values;

import ir.Analysis.LoopInfo;
import ir.MyModule;
import ir.types.FunctionType;
import ir.types.Type;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Function类,代表一个函数，拥有一串基本块，一串参数，一个SymbolTable //
 */
public class Function extends Value {

  //参数声明，不含值
  public class Arg extends Value {

    private int rank;//排第几,非负

    public boolean isMustBeGlobal() {
      return mustBeGlobal;
    }

    public void setMustBeGlobal(boolean mustBeGlobal) {
      this.mustBeGlobal = mustBeGlobal;
    }

    private boolean mustBeGlobal = true;

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

    @Override
    public String toString() {
      return this.getType() + " " + this.getName();
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
    callerList = new ArrayList<>();
    calleeList = new ArrayList<>();
    storeGVSet = new HashSet<>();
    loadGVSet = new HashSet<>();
    buildArgs();
  }

  public Function(String name, Type type, MyModule module) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
    argList_ = new ArrayList<>();
    callerList = new ArrayList<>();
    calleeList = new ArrayList<>();
    storeGVSet = new HashSet<>();
    loadGVSet = new HashSet<>();
    buildArgs();
  }

  public Function(String name, Type type, MyModule module, boolean isBuiltin) {
    super(name, type);
    list_ = new IList<>(this);
    node = new INode<>(this);
    this.node.insertAtEnd(module.__functions);
    argList_ = new ArrayList<>();
    callerList = new ArrayList<>();
    calleeList = new ArrayList<>();
    storeGVSet = new HashSet<>();
    loadGVSet = new HashSet<>();
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getType().getRetType())
        .append(" ")
        .append("@")
        .append(this.getName())
        .append("(");
    this.argList_.forEach(
        arg -> {
          sb.append(arg).append(",");
        }
    );
    if (argList_.size() != 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append(")");
    return sb.toString();
  }

  public INode<Function, MyModule> getNode() {
    return node;
  }

  public ArrayList<Arg> getArgList_() {
    return argList_;
  }

  private boolean isBuiltin_ = false;//lib function
  private IList<BasicBlock, Function> list_;
  private INode<Function, MyModule> node;
  private ArrayList<Arg> argList_;//有序参数列表
  private LoopInfo loopInfo = new LoopInfo(); // 函数内的循环信息
  private ArrayList<Function> callerList; // 调用此函数的所有函数
  private ArrayList<Function> calleeList; // 此函数调用的所有函数

  private HashSet<GlobalVariable> storeGVSet; // 修改的全局 i32 变量集合
  private HashSet<GlobalVariable> loadGVSet; // 使用的全局 i32 变量集合

  public HashSet<GlobalVariable> getStoreGVSet() {
    return storeGVSet;
  }

  public void setStoreGVSet(HashSet<GlobalVariable> storeGVSet) {
    this.storeGVSet = storeGVSet;
  }

  public HashSet<GlobalVariable> getLoadGVSet() {
    return loadGVSet;
  }

  public void setLoadGVSet(HashSet<GlobalVariable> loadGVSet) {
    this.loadGVSet = loadGVSet;
  }

  public ArrayList<Function> getCallerList() {
    return callerList;
  }

  public ArrayList<Function> getCalleeList() {
    return calleeList;
  }

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
