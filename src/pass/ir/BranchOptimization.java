package pass.ir;

import ir.MyModule;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.TerminatorInst.BrInst;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

public class BranchOptimization implements IRPass {
  private Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "branchOptimization";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : BranchOptimization");

    for (var funcNode: m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runBranchOptimization(funcNode.getVal());
      }
    }
  }

  public void runBranchOptimization(Function func) {
    for (var bbNode = func.getList_().getEntry(); bbNode != null; ) {
      var tmp = bbNode.getNext();
      var bb = bbNode.getVal();

      // 1. remove empty bb
      if (bb.getList().getNumNode() == 0) {
        bbNode.removeSelf();
      }

      // 2. merge bb with single unconditional br
      if (bb.getList().getNumNode() == 1) {
        var instruction = bb.getList().getEntry().getVal();
        if (instruction instanceof BrInst && instruction.getNumOP() == 1) {
          var targetBB = instruction.getOperands().get(0);
          for (var predBB : bb.getPredecessor_()) {
            var predInst = predBB.getList().getLast().getVal();
            assert predInst instanceof BrInst;
            switch (predInst.getNumOP()) {
              case 1 -> {
                predInst.CoSetOperand(0, targetBB);
              }
              case 3 -> {
                if (bb == predInst.getOperands().get(1)) {
                  predInst.CoSetOperand(1, targetBB);
                }
                if (bb == predInst.getOperands().get(2)) {
                  predInst.CoSetOperand(2, targetBB);
                }
              }
            }
          }
          bbNode.removeSelf();
        }
      }

      bbNode = tmp;
    }
  }
}
