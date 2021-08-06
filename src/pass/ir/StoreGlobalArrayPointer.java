package pass.ir;

import ir.MyModule;
import ir.Use;
import ir.values.Constants.ConstantArray;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import pass.Pass;
import pass.Pass.IRPass;

//补丁pass，现有的依赖分析方法无法很好处理全局数组的gep,导致每次都要重新load，所以在每个函数里将全局数组地址存入局部
public class StoreGlobalArrayPointer implements IRPass {

  HashMap<GlobalVariable, ArrayList<Function>> gvUserFunc = new HashMap<>();
  MyModule m;

  @Override
  public String getName() {
    return "storeGVPointer";
  }

  @Override
  public void run(MyModule m) {
    this.m = m;
    loadUserFuncs();
    storePointers();
  }

  private void storePointers() {
    for (GlobalVariable gv : m.__globalVariables) {
      if (gv.init instanceof ConstantArray){

      }
    }
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
