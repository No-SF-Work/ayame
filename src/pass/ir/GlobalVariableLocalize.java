package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
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
  MyModule m;
  MyFactoryBuilder f;
  HashSet<Function> recFuncs = new HashSet<>();

  @Override
  public void run(MyModule m) {
    this.m = m;
    this.f = MyFactoryBuilder.getInstance();
    loadUserFuncs();
    findRecursion();

    localize();
  }

  private void localize() {
    for (GlobalVariable gv : m.__globalVariables) {
      var userFuncs = gvUserFunc.get(gv);
      if (gv.init instanceof ConstantInt) {
        //直接局部化
        if (userFuncs.size() == 1) {
          var fun = userFuncs.get(0);
          if (!recFuncs.contains(fun)) {
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

        }
      }
    }
  }

  //在mem2reg之前完成，在funcInline之前完成，需要手动找到递归
  private void findRecursion() {
    for (INode<Function, MyModule> func : m.__functions) {
      var f = func.getVal();
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
    if (recFuncs.contains(target)) {
      return true;
    }
    if (start.getCalleeList().contains(target)) {
      return true;
    }
    for (Function i : start.getCalleeList()) {
      if (!i.isBuiltin_() && findF(i, target)) {
        return true;
      }
    }
    return false;
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
