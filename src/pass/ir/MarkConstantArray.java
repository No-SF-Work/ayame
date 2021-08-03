package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.PointerType;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.StoreInst;
import java.util.HashMap;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

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
        for (var bbNode : funcNode.getVal().getList_()) {
          for (var instNode : bbNode.getVal().getList()) {
            var inst = instNode.getVal();
            switch (inst.tag) {
              case Store -> {
                var pointer = ArrayAliasAnalysis.getArrayValue(((StoreInst) inst).getPointer());
                if (hasModify.containsKey(pointer)) {
                  hasModify.replace((GlobalVariable) pointer, true);
                }
              }
              case Call -> {
                var callFunc = (Function) inst.getOperands().get(0);
                if (callFunc.isHasSideEffect()) {
                  for (var arg: inst.getOperands()) {
                    // FIXME call 传数组还有 Load，等内联修复
                    if (arg instanceof GEPInst) {
                      var pointer = ArrayAliasAnalysis.getArrayValue(((GEPInst) arg).getOperands().get(0));
                      if (hasModify.containsKey(pointer)) {
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

    for (var gv: hasModify.keySet()) {
      if (!hasModify.get(gv)) {
        gv.setConst();
      }
    }

  }
}
