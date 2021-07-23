package pass.ir;

import ir.MyModule;
import ir.values.Function;
import ir.values.instructions.Instruction;
import pass.Pass.IRPass;
import util.Mylogger;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

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
    for (var op: instruction.getOperands()) {
      if (op instanceof Instruction) {
        findUsefulClosure((Instruction) op);
      }
    }
  }

  public void run(MyModule m) {
    log.info("Running pass : DeadCodeEmit");
    for (var funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runDCE(funcNode.getVal());
      }
    }
  }

  public void runDCE(Function func) {
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

    for (var bbNode: func.getList_()) {
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
