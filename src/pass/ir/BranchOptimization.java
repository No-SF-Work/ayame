package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.TerminatorInst.BrInst;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

public class BranchOptimization implements IRPass {

  private Logger log = Mylogger.getLogger(IRPass.class);
  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  @Override
  public String getName() {
    return "branchOptimization";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : BranchOptimization");

    for (var funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runBranchOptimization(funcNode.getVal());
      }
    }
  }

  public void runBranchOptimization(Function func) {
    boolean changed = true;

    while (changed) {
      changed = false;

      for (var bbNode = func.getList_().getEntry(); bbNode != null; ) {
        var tmp = bbNode.getNext();
        var bb = bbNode.getVal();

        // 1. remove empty bb
        if (bb.getList().getNumNode() == 0) {
          bbNode.removeSelf();
          changed = true;
        }

        // 2. merge conditional br if both op are same
        if (bb.getList().getLast().getVal() instanceof BrInst) {
          BrInst brInst = (BrInst) bb.getList().getLast().getVal();
          if (brInst.getNumOP() == 3 && (brInst.getOperands().get(1) == brInst.getOperands()
              .get(2))) {
            brInst.node.removeSelf();
            factory.buildBr((BasicBlock) brInst.getOperands().get(1), bb);
            changed = true;
          }
        }

        // 3. merge bb with single unconditional br
        if (bb.getList().getNumNode() == 1) {
          var instruction = bb.getList().getEntry().getVal();
          if (instruction instanceof BrInst && instruction.getNumOP() == 1) {
            var targetBB = instruction.getOperands().get(0);
//          if (((BasicBlock) targetBB).getList().getEntry().getVal().tag == TAG_.Phi) {
//            // TODO: br 直接跳到 phi 会有问题
//            bbNode = tmp;
//            continue;
//          }
            for (var predBB : bb.getPredecessor_()) {
              var predInst = predBB.getList().getLast().getVal();
              assert predInst instanceof BrInst;
              switch (predInst.getNumOP()) {
                case 1 -> {
                  predInst.CoSetOperand(0, targetBB);
                  changed = true;
                }
                case 3 -> {
                  if (bb == predInst.getOperands().get(1)) {
                    predInst.CoSetOperand(1, targetBB);
                    changed = true;
                  }
                  if (bb == predInst.getOperands().get(2)) {
                    predInst.CoSetOperand(2, targetBB);
                    changed = true;
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
}
