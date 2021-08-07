package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.PointerType;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.StoreInst;
import pass.Pass.IRPass;
import util.Mylogger;

import java.util.HashMap;
import java.util.logging.Logger;

public class MarkConstantArray implements IRPass {

  private Logger log = Mylogger.getLogger(IRPass.class);
  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private HashMap<GlobalVariable, Boolean> hasModify = new HashMap<GlobalVariable, Boolean>();

  @Override
  public String getName() {
    return "markConstantArray";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : BranchOptimization");

    for (var gv : m.__globalVariables) {
      if (!gv.isConst && ((PointerType) gv.getType()).getContained().isArrayTy()) {
        hasModify.put(gv, false);
      }
    }

    for (var funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runOnFunction(funcNode.getVal());
      }
    }

    for (var gv : hasModify.keySet()) {
      if (!hasModify.get(gv)) {
        gv.setConst();
      }
    }

  }

  public void runOnFunction(Function func) {
    for (var bbNode : func.getList_()) {
      for (var instNode : bbNode.getVal().getList()) {
        var inst = instNode.getVal();
        switch (inst.tag) {
          case Store -> {
            var pointer = ArrayAliasAnalysis.getArrayValue(((StoreInst) inst).getPointer());
            if (pointer instanceof GlobalVariable && hasModify.containsKey(pointer)) {
              hasModify.replace((GlobalVariable) pointer, true);
            }
          }
          case Call -> {
            var callFunc = (Function) inst.getOperands().get(0);
            if (callFunc.isHasSideEffect()) {
              for (var arg : inst.getOperands()) {
                // FIXME call 传数组还有 Load，等内联修复
                if (arg instanceof GEPInst) {
                  var pointer = ArrayAliasAnalysis
                      .getArrayValue(((GEPInst) arg).getOperands().get(0));
                  if (pointer instanceof GlobalVariable && hasModify.containsKey(pointer)) {
                    hasModify.replace((GlobalVariable) pointer, true);
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
