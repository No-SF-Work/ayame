package pass.ir;

import ir.MyModule;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;
import java.util.ArrayList;
import java.util.HashMap;
import pass.Pass.IRPass;

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
   *   3.只读不写的全局变量
   *   4.只有一个user,这个user在非递归函数里
   *  b:间接局部化
   *   1.全局变量在循环里面被反复du,就将其局部化
   *
   * */
  HashMap<GlobalVariable, ArrayList<Function>> gvUserFunc = new HashMap<>();
  MyModule m;

  @Override
  public void run(MyModule m) {
    this.m = m;
  loadUserFuncs();

  }

  public void loadUserFuncs() {
    m.__globalVariables.forEach(
        gv -> {
          ArrayList<Function> parents = new ArrayList<>();
          gv.getUsesList().forEach(
              use -> {
                //fixme 被内联的函数调用没有删除user关系，记得删除
                parents.add(((Instruction) use.getUser()).getBB().getParent());

              }
          );
          gvUserFunc.put(gv, parents);
        }
    );
  }

}
