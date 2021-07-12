package ir;

import driver.Config;
import ir.types.ArrayType;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.types.Type.LabelType;
import ir.types.Type.VoidType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.MemInst.ZextInst;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.CallInst;
import ir.values.instructions.TerminatorInst.RetInst;
import java.util.ArrayList;
import java.util.logging.Logger;
import util.Mylogger;

/**
 * get开头的方法是工厂方法，返回的是没有被持有的
 * <p>
 * build开头的方法是build方法，一般就三种
 * <p>
 * 1. build ->在你传入的bb的末尾插入一个这个指令
 * <p>
 * 2.buildAfter->在你传入的Inst的后面插入这个指令
 * <p>
 * 3.buildBefore->前面插入
 * <p>
 * 4.调用就行了，bb里和func里的链表会自动维护的
 * <p>
 * 5.***当你使用getX的时候，请确保你知道自己在干什么，
 * <p>
 * 在你需要放置这个Value的时候，你需要自己把这个Value插入到适当的容器里面的适当的位置***
 **/

public class MyFactoryBuilder {

  Logger log = Mylogger.getLogger(MyFactoryBuilder.class);

  private MyFactoryBuilder() {
  }


  private static MyFactoryBuilder mf = new MyFactoryBuilder();

  public static MyFactoryBuilder getInstance() {
    return mf;
  }

  public ConstantInt getConstantInt(int val) {
    return ConstantInt.newOne(IntegerType.getI32(), val);
  }

  public ConstantArray getConstantArray(Type type, ArrayList<Constant> arr) {
    return new ConstantArray(type, arr);
  }

  public GlobalVariable getGlobalvariable(String name, Type type, Constant init) {
    return new GlobalVariable(name, type, init);
  }

  public ConstantInt CONST0() {
    return ConstantInt.CONST0();//type is i32
  }

  public VoidType getVoidTy() {
    return VoidType.getType();
  }

  public LabelType getLabelTy() {
    return LabelType.getType();
  }

  public IntegerType getI32Ty() {
    return IntegerType.getI32();
  }

  public IntegerType getI1Ty() {
    return IntegerType.getI1();
  }

  /**
   * @param contained:指向元素的类型
   */
  public PointerType getPointTy(Type contained) {
    return new PointerType(contained);
  }

  /**
   * @param retTy:返回值类型
   * @param params:参数类型的列表
   */
  public FunctionType getFuncTy(Type retTy, ArrayList<Type> params) {

    return new FunctionType(retTy, params);
  }

  public ArrayType getArrayTy(Type containedTy, int numElem) {
    return new ArrayType(containedTy, numElem);
  }

  public CallInst buildFuncCall(Function func, ArrayList<Value> args, BasicBlock bb) {
    return new CallInst(func, args, bb);
  }

  //获得一个function
  public Function getFunction(String name, Type functype) {
    log.info("new Function : " + name + " return type :" + functype);
    return new Function(name, functype);
  }

  public Function getBuiltInFunc(String name, Type funcTy) {
    return new Function(name, funcTy, MyModule.getInstance(), true);
  }

  //只有一个module，在module末尾插入function
  public Function buildFunction(String name, Type functype) {
    log.info("new Function : " + name + " return type :" + functype);
    return new Function(name, functype, MyModule.getInstance());
  }

  // 内置函数，isBuiltin 为 true，其他函数用上面的方法 build
  public Function buildFunction(String name, Type functype, boolean isBuiltin) {
    log.info("new Function : " + name + " return type :" + functype + " isBuiltin");
    return new Function(name, functype, MyModule.getInstance(), true);
  }

  //获得一个BasicBlock
  public BasicBlock getBasicBlock(String name) {
    return new BasicBlock(name);
  }

  //在func末尾插入bb
  public BasicBlock buildBasicBlock(String name, Function func) {
    return new BasicBlock(name, func);
  }

  /**
   * 返回一个不被持有的Binary，需要指定tag
   */
  public BinaryInst getBinary(TAG_ tag, Value lhs, Value rhs) {
    assert lhs.getType() == rhs.getType();
    return new BinaryInst(tag, lhs.getType(), lhs, rhs);
  }

  /**
   * 在inst后面造一个binary
   *
   * @param tag:手动标记的binary运算的类型
   */
  public BinaryInst buildBinaryAfter(TAG_ tag, Value lhs, Value rhs, Instruction inst) {
    assert lhs.getType() == rhs.getType();
    return new BinaryInst(tag, lhs.getType(), lhs, rhs, inst);
  }

  /**
   * 在bb末尾插入个binary
   */
  public BinaryInst buildBinary(TAG_ tag, Value lhs, Value rhs, BasicBlock bb) {
    assert lhs.getType() == rhs.getType();
    return new BinaryInst(tag, lhs.getType(), lhs, rhs, bb);
  }

  /**
   * 在Inst前面造一个Binary
   */
  public BinaryInst buildBinaryBefore(Instruction inst, TAG_ tag, Value lhs, Value rhs) {
    assert lhs.getType() == rhs.getType();
    return new BinaryInst(inst, tag, lhs.getType(), lhs, rhs);
  }

  /**
   * 在bb末尾造一个无条件转移
   */
  public BrInst buildBr(BasicBlock trueblock, BasicBlock parent) {
    return new BrInst(trueblock, parent);
  }

  /**
   * 在bb末尾造一个条件转移
   */
  public BrInst buildBr(Value cond, BasicBlock trueblock, BasicBlock falseBlock,
      BasicBlock parent) {
    return new BrInst(cond, trueblock, falseBlock, parent);
  }

  /**
   * 在bb末尾造一个return void
   */
  public RetInst buildRet(BasicBlock bb) {
    return new RetInst(bb);
  }

  /**
   * 在bb末尾造一个 return i32
   */
  public RetInst buildRet(Value val, BasicBlock bb) {
    return new RetInst(val, bb);
  }

  /**
   * 返回一个Alloca
   *
   * @param type:申请的指针的type
   */
  public AllocaInst getAlloca(Type type) {
    return new AllocaInst(type);
  }

  /**
   * 以不同的情况build
   */
  public AllocaInst buildAlloca(BasicBlock bb, Type type) {
    var t = new AllocaInst(type);
    //
    bb.getParent().getList_().getEntry()
        .getVal().getList().getEntry().insertBefore(t.node);
    return t;
  }

  public AllocaInst buildAllocaBefore(Type type, Instruction inst) {
    return new AllocaInst(type, inst);
  }

  public AllocaInst buildAllocaAfter(Type type, Instruction inst) {
    return new AllocaInst(inst, type);
  }

  /**
   * @param type:要的值的type
   * @param value:想要取的指针  返回一个Load
   */

  public LoadInst getLoad(Type type, Value value) {
    return new LoadInst(type, value);
  }

  public LoadInst buildLoad(Type type, Value value, BasicBlock bb) {
    return new LoadInst(type, value, bb);
  }

  public LoadInst buildAfter(Type type, Value value, Instruction prev) {
    return new LoadInst(type, value, prev);
  }

  public LoadInst buildBefore(Type type, Value value, Instruction next) {
    return new LoadInst(next, value, type);
  }

  /**
   * @param val:值
   * @param pointer:指针
   */
  public StoreInst getStore(Value val, Value pointer) {
    return new StoreInst(val, pointer);
  }

  public StoreInst buildStore(Value val, Value pointer, BasicBlock bb) {
    return new StoreInst(val, pointer, bb);
  }

  public StoreInst buildStoreAfter(Value val, Value pointer, Instruction prev) {
    return new StoreInst(val, pointer, prev);
  }

  public StoreInst buildStoreBefore(Value val, Value pointer, Instruction next) {
    return new StoreInst(next, val, pointer);
  }

  /**
   * @param ptr:     你想要对其操作的pointer,举个例子，在你想要取出数组a[2][3]中的某个值的时候，你传进来的这个就是 a 的指针
   * @param indices: 具体求的位置，因为在SysY中没有结构体什么的复杂结构，所以可以认为这就是数组的每个维度的值 比如 a[2][3] ，这里传进来的就是["i"]["j"]
   */
  public GEPInst getGEP(Value ptr, ArrayList<Value> indices) {
    return new GEPInst(ptr, indices);
  }

  public GEPInst buildGEP(Value ptr, ArrayList<Value> indices, BasicBlock parent) {
    return new GEPInst(ptr, indices, parent);
  }

  public GEPInst buildGEPAfter(Value ptr, ArrayList<Value> indices, Instruction prev) {
    return new GEPInst(prev, ptr, indices);
  }

  public GEPInst buildGEPBefore(Value ptr, ArrayList<Value> indices, Instruction next) {
    return new GEPInst(ptr, indices, next);
  }

  /***/

  /*
   *@param value:原value
   *@param dest:想要转为的类型
   */
  public ZextInst buildZext(Value value, Type dest, BasicBlock parent) {
    return new ZextInst(value, dest, parent);
  }
}
