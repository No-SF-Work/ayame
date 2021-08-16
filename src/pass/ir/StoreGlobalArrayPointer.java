package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.Constants.ConstantArray;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.AllocaInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import pass.Pass;
import pass.Pass.IRPass;

//补丁pass，现有的依赖分析方法无法很好处理全局数组的gep,导致每次都要重新load，所以在每个函数里将全局数组地址存入局部
public class StoreGlobalArrayPointer implements IRPass {

  HashMap<GlobalVariable, ArrayList<Function>> gvUserFunc = new HashMap<>();
  MyModule m;
  MyFactoryBuilder f = MyFactoryBuilder.getInstance();

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
      if (((PointerType) gv.getType()).getContained() instanceof ArrayType) {
        var userFuncs = gvUserFunc.get(gv);
        for (Function userFunc : userFuncs) {
          var entry = userFunc.getList_().getEntry();
          var alloca = f.buildAlloca(entry.getVal(), gv.getType());
          var iter = entry.getVal().getList().getEntry();
          while (iter.getNext() != null && iter.getNext().getVal() instanceof AllocaInst) {
            iter = iter.getNext();
          }

          var load = f.buildLoadAfter(gv.getType(), alloca, iter.getVal());
          var size = gv.getUsesList().size();
          ArrayList<Use> tobeReplace = new ArrayList<>();
          for (int i = 0; i < size; i++) {
            var use = gv.getUsesList().get(i);
            if (((Instruction) use.getUser()).getBB().getParent().equals(userFunc)) {
              tobeReplace.add(use);
            }
          }
          for (Use use : tobeReplace) {
            var user = use.getUser();
            var rank = use.getOperandRank();
            use.getValue().CORemoveUse(use);
            user.CoSetOperand(rank, load);
            use.getUser();
          }
          var it = entry.getVal().getList().getEntry();
          while (it.getNext() != null && it.getNext().getVal() instanceof AllocaInst) {
            it= it.getNext();
          }
          var store = f.buildStoreAfter(gv, alloca, it.getVal());
        }
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
          parents = (ArrayList<Function>) parents.stream().distinct()
              .collect(Collectors.toList());
          gvUserFunc.put(gv, parents);
        }
    );
  }
}
