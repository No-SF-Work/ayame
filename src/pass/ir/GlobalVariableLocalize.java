package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.values.BasicBlock;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.AllocaInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.stream.Collectors;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Pair;

public class GlobalVariableLocalize implements IRPass {

  @Override
  public String getName() {
    return "gvlocalize";
  }

  /*
   * 将热点全局变量局部化
   * todo
   *  a:直接局部化
   *   1.只被main函数使用的全局变量
   *   2.只被只调用一次的函数使用的全局变量
   *  b:间接局部化（保证局部性）
   *   1.全局变量在循环里面被反复du,就将其局部化
   *
   * */
  HashMap<GlobalVariable, ArrayList<Function>> gvUserFunc = new HashMap<>();
  //一个函数通过调用关系bfs出去，如果用到了gv就认为是related
  HashMap<Function, HashSet<GlobalVariable>> relatedGVs = new HashMap<>();
  MyModule m;
  MyFactoryBuilder f;
  HashSet<Function> recFuncs = new HashSet<>();
  HashSet<Function> visitfunc = new HashSet<>();

  @Override
  public void run(MyModule m) {
    this.m = m;
    this.f = MyFactoryBuilder.getInstance();
    loadUserFuncs();
    findRecursion();
    findRelatedFunc();
    localize();
  }

  private void localize() {
    for (GlobalVariable gv : m.__globalVariables) {
      var userFuncs = gvUserFunc.get(gv);
      if (gv.init instanceof ConstantInt) {
        //直接局部化
        if (userFuncs.size() == 1) {
          var fun = userFuncs.get(0);
          if (!recFuncs.contains(fun) && fun.getName().equals("main")) {
            var entry = fun.getList_().getEntry();
            var alloca = f.buildAlloca(entry.getVal(), f.getI32Ty());
            var iter = entry.getVal().getList().getEntry();
            while (iter.getNext() != null && iter.getNext().getVal() instanceof AllocaInst) {
              iter = iter.getNext();
            }
            f.buildStoreAfter(gv.init, alloca, iter.getVal());
            gv.COReplaceAllUseWith(alloca);
          }
        } else {//间接局部化
          /*todo
              1.使用了gv的函数中把gv换掉
              2.在退出的时候store
          * */
          for (Function func : userFuncs) {
            localizeOneFunc(gv, func);
          }
        }
      }
    }
  }

  private void localizeOneFunc(GlobalVariable gv, Function f) {
    /*todo
       1.找到所有退出的点(ret|call)
       2.对于ret,都进行
       3.对于所有call,如果是related就store,不是related就不store（对于一些拿全局变量当迭代器的恶心测试点，这里能起到比较好的效果）
    */






  }

  //找related gvs 的目的是：在退出函数的时候，需要进行一个store把局部的值存回gv
//在通过call退出的时候，如果gv和call的func的所有callee以及callee的callee都无关系，可以省去一条store
  private boolean bfsFuncs(Function start, GlobalVariable gv) {
    if (visitfunc.contains(start)) {
      return false;
    }
    visitfunc.add(start);
    if (gvUserFunc.get(gv).contains(start)) {
      return true;
    }
    var result = false;
    for (Function callee : start.getCalleeList()) {
      result |= bfsFuncs(callee, gv);
    }
    return result;
  }


  private void findRelatedFunc() {
    for (INode<Function, MyModule> function : m.__functions) {
      var val = function.getVal();
      relatedGVs.put(val, new HashSet<>());
      for (GlobalVariable globalVariable : m.__globalVariables) {
        visitfunc.clear();
        if (bfsFuncs(val, globalVariable)) {
          relatedGVs.get(val).add(globalVariable);
        }
      }
    }
  }

  //在mem2reg之前完成，在funcInline之前完成，需要手动找到递归
  private void findRecursion() {
    for (INode<Function, MyModule> func : m.__functions) {
      var f = func.getVal();
      visitfunc.clear();
      if (!f.isBuiltin_() && findF(f, f)) {
        recFuncs.add(f);
      }
    }
    var size = 0;
    do {
      size = recFuncs.size();
      for (Function func : recFuncs) {
        recFuncs.addAll(func.getCalleeList());
      }
    } while (size != recFuncs.size());
  }

  private boolean findF(Function start, Function target) {
    if (visitfunc.contains(start)) {
      return false;
    }
    visitfunc.add(start);
    if (start.getCalleeList().contains(target)) {
      return true;
    }
    var result = false;
    for (Function function : start.getCalleeList()) {
      result |= findF(function, target);
    }
    return result;
  }


  private void loadUserFuncs() {
    m.__globalVariables.forEach(
        gv -> {
          ArrayList<Function> parents = new ArrayList<>();
          for (Use use : gv.getUsesList()) {
            var func = ((Instruction) use.getUser()).getBB().getParent();
            if (!(func.getCallerList().isEmpty() && !func.getName().equals("main"))) {
              parents.add(((Instruction) use.getUser()).getBB().getParent());
            }
          }
          parents = (ArrayList<Function>) parents.stream().distinct().collect(Collectors.toList());
          gvUserFunc.put(gv, parents);
        }
    );
  }

}
