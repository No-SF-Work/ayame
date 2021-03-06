package pass.ir;

import ir.MyModule;
import ir.Use;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.CallInst;
import ir.values.instructions.TerminatorInst.RetInst;
import java.util.ArrayList;
import java.util.HashSet;
import pass.Pass.IRPass;
import util.IList.INode;

public class InterProceduralDCE implements IRPass {


  @Override
  public String getName() {
    return "interproceduraldce";
  }

  /*
   * todo
   *  这个pass需要在mem2reg之前调用
   *   1.跨过程的数据流好麻啊,失败了失败了失败了失败了失败了
   *   2.如果一个函数的返回值实际上没被使用过，那么标记这个函数，并且把其所有返回值更换为CONST0
   *    :假设：使用了与输出的BUILTIN函数相关的函数的与被BUILTIN函数使用的所有值都是不可删除的
   *    :跑这个之前跑一次inter procedural ana与bb pred succ
   *    :如果一个函数的所有返回值在所有地方都没有用到（递归函数的情况要进行特判），就把这个函数的所有返回值替换为Const0并且接触Ret的关系
   *    :对每个gv判断其在每个函数里面是否有用(指通过use关系和输出函数相连接)，如果在所有函数里面都没用，就把这个gv删除
   *    根据去年median的尿性，我猜测会
   *        1.出个一个函数有两个出口，一个是对自己的尾调用，一个是返回一个p用没有但是算了一堆的东西
   *        2.出个没有用但是在很多函数间计算了好多次的全局变量
   *
   * */
  MyModule m;
  HashSet<Function> funcs = new HashSet<>();//输出函数
  HashSet<Function> needtokeep = new HashSet<>();
  HashSet<Value> cd = new HashSet<>();//can't be deleted
  private boolean changed = false;
  HashSet<Function> optedFunc = new HashSet<>();

  @Override
  public void run(MyModule m) {
    this.m = m;
    for (INode<Function, MyModule> node : m.__functions) {
      var name = node.getVal().getName();
      if (name.equals("putch") || name.equals("putarray") || name.equals("putint")) {
        funcs.add(node.getVal());
        cd.add(node.getVal());
      }
    }
    do {
      changed = false;
      removeUseLessRet();
      removeUselessGV();
    } while (changed);
  }

  //我们认为只有通过use关系向下搜索能够搜索到putch,putarray,putint,br,ret,call的全局变量是"有用"的全局变量
  //对于ret,跟进分析返回值与传入参数的使用情况(麻)
  //对于"没用"的全局变量，保留他们的getint getch getarray(虽然应该没有出题人犯病输入一堆没用的数据，但只要有，那就赚了)
  private void removeUselessGV() {
    for (GlobalVariable __globalVariable : m.__globalVariables) {
      analyseGv(__globalVariable);
      if (happy) {
        relatedValues.forEach(value -> {
          if (value instanceof Instruction) {
            ((Instruction) value).CORemoveAllOperand();
            ((Instruction) value).node.removeSelf();
          }
        });
      }
    }
  }

  private void funcArgsPreAnalyse() {

  }

  private ArrayList<Function> isUsefulInFunc = new ArrayList<>();
  HashSet<Value> relatedValues = new HashSet<>();
  ArrayList<RetInst> relatedRet = new ArrayList<>();
  ArrayList<CallInst> relatedCall = new ArrayList<>();
  ArrayList<BrInst> relatedBr = new ArrayList<>();
  HashSet<Function> relatedFunc = new HashSet<>();
  boolean happy = true;

  private void analyseGv(GlobalVariable gv) {//
    happy = true;
    relatedValues.clear();
    relatedRet.clear();
    relatedCall.clear();
    relatedBr.clear();
    findRelatedUsers(gv);

    for (Function f : funcs) {
      if (relatedFunc.contains(f)) {
        happy = false;
        return;
      }
    }
    if (!relatedBr.isEmpty()) {
      happy = false;
      return;
    }
    if (!relatedFunc.isEmpty()) {
      happy = false;
      return;
    }
    if (!relatedCall.isEmpty()) {
      happy = false;
      return;
    }
    if (!relatedRet.isEmpty()) {
      happy = false;
      return;
    }
  }

  private void findRelatedUsers(Value value) {
    if (relatedValues.contains(value)) {
      return;
    }
    if (value instanceof StoreInst) {//store要把user的两个operand都放进去
      /*
       * store @a pointer
       * a和pointer有关联
       * */
      if (((StoreInst) value).getPointer() instanceof GEPInst) {
        findRelatedUsers(((GEPInst) ((StoreInst) value).getPointer()).getAimTo());
        findRelatedUsers(((StoreInst) value).getVal());
      }

    }
    if (value instanceof RetInst) {
      relatedRet.add((RetInst) value);
    }
    if (value instanceof CallInst) {
      relatedFunc.add(((CallInst) value).getFunc());
      relatedCall.add((CallInst) value);
    }
    if (value instanceof BrInst) {
      relatedBr.add((BrInst) value);
    }

    relatedValues.add(value);
    value.getUsesList().forEach(use -> {
      findRelatedUsers(use.getUser());
    });
  }


  /*
          x没用但是函数内的dce消不掉
          int t=2;
          * int foo(int j){
          * int x =100;
          * t=t+1;
          * for(x<100 x=x+1);
          *    if(j==0){
          *   return x;
          * }
          * return foo(j-1);
          * }
          * int main(){
          * foo(114514);
          * return t;
          * }
          * */
  //找到返回值没用的函数，把所有返回值改成ret0
  //remove use less ret 和inter procedural gv dce 组合起来后
  //实际上能够起到跨过程的局部变量的dce的作用，因为一个局部变量，能够和调用者交流的方式只有gv或者是ret
  private void removeUseLessRet() {
    for (INode<Function, MyModule> fnd : m.__functions) {
      var fun = fnd.getVal();
      if (!fun.isBuiltin_() && fun.getType().getRetType().isIntegerTy()) {
        if (!fun.getName().equals("main")){
          processOneFunc(fun);
        }
      }
    }
  }

  private void processOneFunc(Function fun) {
    var isUseful = false;
    var recursion = false;
    ArrayList<CallInst> innerCall = new ArrayList<>();
    if (optedFunc.contains(fun)) {
      return;
    }
    //analyse all call
    for (Use use : fun.getUsesList()) {
      var inst = use.getUser();
      if (inst instanceof CallInst) {
        if (((CallInst) inst).getBB().getParent().equals(fun)) {
          recursion = true;
          innerCall.add(((CallInst) inst));
        } else {
          if (!inst.getUsesList().isEmpty()) {
            isUseful = true;
          }
        }
      }
    }
    if (isUseful) {
      return;
    }
    if (recursion) {
      for (CallInst call : innerCall) {
        if (call.getUsesList().size() > 1) {
          return;
        } else {
          if (!call.getUsesList().isEmpty()) {
            if (!(call.getUsesList().get(0).getUser() instanceof RetInst)) {
              return;
            }
          }
        }
      }
      //ok 是我们想要的形式
      changed = true;
      optedFunc.add(fun);
      fun.getList_().forEach(
          bb -> {
            bb.getVal().getList().forEach(instnd -> {
              var val = instnd.getVal();
              if (val instanceof RetInst) {
                val.CORemoveAllOperand();
                val.COaddOperand(ConstantInt.CONST0());
              }
            });
          }
      );
    } else {
      for (CallInst call : innerCall) {
        if (call.getUsesList().size() > 1) {
          return;
        } else {
          if (!call.getUsesList().isEmpty()) {
            if (!(call.getUsesList().get(0).getUser() instanceof RetInst)) {
              return;
            }
          }
        }
      }
      fun.getList_().forEach(
          bb -> {
            bb.getVal().getList().forEach(instnd -> {
              var val = instnd.getVal();
              if (val instanceof RetInst) {
                val.CORemoveAllOperand();
                val.COaddOperand(ConstantInt.CONST0());
              }
            });
          }
      );
    }
  }
}
