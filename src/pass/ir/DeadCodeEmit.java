package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyModule;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

/**
 * 根据 Br, Ret, Store, 有副作用函数的 Call 及其操作数求出一个闭包，删掉闭包外的指令
 */
public class DeadCodeEmit implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);
  Set<Instruction> usefulInstSet = new HashSet<>();

  @Override
  public String getName() {
    return "deadcodeemit";
  }

  public boolean isUseful(Instruction instruction) {
    return switch (instruction.tag) {
      case Br, Ret, Store -> true;
      case Call -> ((Function) (instruction.getOperands().get(0))).isHasSideEffect();
      default -> false;
    };
  }

  public void findUsefulClosure(Instruction instruction) {
    if (usefulInstSet.contains(instruction)) {
      return;
    }
    usefulInstSet.add(instruction);
    for (var op : instruction.getOperands()) {
      if (op instanceof Instruction) {
        findUsefulClosure((Instruction) op);
      }
    }
  }

  public void run(MyModule m) {
    log.info("Running pass : DeadCodeEmit");
    ArrayList<Function> uselessFuncs = new ArrayList<>();
    for (var funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runDCE(funcNode.getVal());
      }
      if (funcNode.getVal().getCallerList().isEmpty() && !funcNode.getVal().getName()
          .equals("main")) {
        uselessFuncs.add(funcNode.getVal());
      }
    }
    for (Function uselessFunc : uselessFuncs) {
      uselessFunc.getNode().removeSelf();
    }
  }

  public void removeUselessStore(Function func) {
    for (var bbNode : func.getList_()) {
      for (var instNode = bbNode.getVal().getList().getEntry(); instNode != null; ) {
        var next = instNode.getNext();
        var inst = instNode.getVal();
        if (inst instanceof StoreInst) {
          var pointer = ArrayAliasAnalysis.getArrayValue(((StoreInst) inst).getPointer());
          for (var ninstNode = next; ninstNode != null; ) {
            var nnext = ninstNode.getNext();
            var ninst = ninstNode.getVal();
            if (ninst.tag == TAG_.Store) {
              if (inst.getOperands().get(0) == ninst.getOperands().get(0)
                  && inst.getOperands().get(1) == ninst.getOperands().get(1)) {
                inst.CORemoveAllOperand();
                inst.node.removeSelf();
                break;
              }
            } else if (ninst.tag == TAG_.Load) {
              var npointer = ArrayAliasAnalysis.getArrayValue(ninst.getOperands().get(0));
              if (ArrayAliasAnalysis.alias(pointer, npointer)) {
                break;
              }
            } else if (ninst.tag == TAG_.Call) {
              if (ArrayAliasAnalysis.callAlias(pointer, (CallInst) ninst)) {
                break;
              }
            }
            ninstNode = nnext;
          }
        }
        instNode = next;
      }
    }
  }

  public void runDCE(Function func) {
    removeUselessStore(func);

    usefulInstSet.clear();
    for (var bbNode : func.getList_()) {
      var bb = bbNode.getVal();
      for (var instNode : bb.getList()) {
        var instruction = instNode.getVal();
        if (isUseful(instruction)) {
          findUsefulClosure(instruction);
        }
      }
    }

    for (var bbNode : func.getList_()) {
      var bb = bbNode.getVal();
      for (var instNode = bb.getList().getEntry(); instNode != null; ) {
        var tmp = instNode.getNext();
        var instruction = instNode.getVal();
        if (!usefulInstSet.contains(instruction)) {
          instruction.CORemoveAllOperand();
          instruction.COReplaceAllUseWith(null);
          instruction.node.removeSelf();
        }
        instNode = tmp;
      }
    }
  }
}
