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
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.BrInst;
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


  //获得一个function
  public Function getFunction(String name, Type functype) {
    log.info("new Function : " + name + " return type :" + functype);
    return new Function(name, functype);
  }

  //只有一个module，在module末尾插入function
  public void buildFunction(String name, Type functype) {
    log.info("new Function : " + name + " return type :" + functype);
    new Function(name, functype, MyModule.getInstance());
  }

  //获得一个BasicBlock
  public BasicBlock getBasicBlock(String name) {
    return new BasicBlock(name);
  }

  //在func末尾插入bb
  public void buildBasicBloock(String name, Function func) {
    new BasicBlock(name, func);
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
   * @param tag:手动标记的binary运算的类型
   */
  public void buildBinaryAfter(TAG_ tag, Value lhs, Value rhs, Instruction inst) {
    assert lhs.getType() == rhs.getType();
    new BinaryInst(tag, lhs.getType(), lhs, rhs, inst);
  }

  /**
   * 在bb末尾插入个binary
   */
  public void buildBinary(TAG_ tag, Value lhs, Value rhs, BasicBlock bb) {
    assert lhs.getType() == rhs.getType();
    new BinaryInst(tag, lhs.getType(), lhs, rhs, bb);
  }

  /**
   * 在Inst前面造一个Binary
   */
  public void buildBinaryBefore(Instruction inst, TAG_ tag, Value lhs, Value rhs) {
    assert lhs.getType() == rhs.getType();
    new BinaryInst(inst, tag, lhs.getType(), lhs, rhs);
  }

  /**
   * 在bb末尾造一个无条件转移
   */
  public void buildBr(BasicBlock trueblock, BasicBlock parent) {
    new BrInst(trueblock, parent);
  }

  /**
   * 在bb末尾造一个条件转移
   */
  public void buildBr(Value cond, BasicBlock trueblock, BasicBlock falseBlock, BasicBlock parent) {
    new BrInst(cond, trueblock, falseBlock, parent);
  }

  /**
   * 在bb末尾造一个return void
   */
  public void buildRet(BasicBlock bb) {
    new RetInst(bb);
  }

  /**
   * 在bb末尾造一个 return i32
   */
  public void buildRet(Value val, BasicBlock bb) {
    new RetInst(val, bb);
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
  public void buildAlloca(BasicBlock bb, Type type) {
    new AllocaInst(bb, type);
  }

  public void buildAllocaBefore(Type type, Instruction inst) {
    new AllocaInst(type, inst);
  }

  public void buildAllocaAfter(Type type, Instruction inst) {
    new AllocaInst(inst, type);
  }

  /**
   * @param type:要的值的type
   * @param value:想要取的指针  返回一个Load
   */

  public LoadInst getLoad(Type type, Value value) {
    return new LoadInst(type, value);
  }

  public void buildLoad(Type type, Value value, BasicBlock bb) {
    new LoadInst(type, value, bb);
  }

  public void buildAfter(Type type, Value value, Instruction prev) {
    new LoadInst(type, value, prev);
  }

  public void buildBefore(Type type, Value value, Instruction next) {
    new LoadInst(next, value, type);
  }

  /**
   * @param val:值
   * @param pointer:指针
   */
  public StoreInst getStore(Value val, Value pointer) {
    return new StoreInst(val, pointer);
  }

  public void buildStore(Value val, Value pointer, BasicBlock bb) {
    new StoreInst(val, pointer, bb);
  }

  public void buildStoreAfter(Value val, Value pointer, Instruction prev) {
    new StoreInst(val, pointer, prev);
  }

  public void buildStoreBefore(Value val, Value pointer, Instruction next) {
    new StoreInst(next, val, pointer);
  }

  /**
   * @param ptr:     你想要对其操作的pointer,举个例子，在你想要取出数组a[2][3]中的某个值的时候，你传进来的这个就是 a 的指针
   * @param indices: 具体求的位置，因为在SysY中没有结构体什么的复杂结构，所以可以认为这就是数组的每个维度的值 比如 a[2][3] ，这里传进来的就是["i"]["j"]
   */
  public GEPInst getGEP(Value ptr, ArrayList<Value> indices) {
    return new GEPInst(ptr, indices);
  }

  public void buildGEP(Value ptr, ArrayList<Value> indices, BasicBlock parent) {
    new GEPInst(ptr, indices, parent);
  }

  public void buildGEPAfter(Value ptr, ArrayList<Value> indices, Instruction prev) {
    new GEPInst(prev, ptr, indices);
  }

  public void buildGEPBefore(Value ptr, ArrayList<Value> indices, Instruction next) {
    new GEPInst(ptr, indices, next);
  }
  /***/


}
