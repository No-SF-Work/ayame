package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.Phi;
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
    while (true) {
      boolean completed = true;

      completed = onlyOneUncondBr(func);
      completed &= endWithUncondBr(func);
      completed &= removeDeadBB(func);

      if (completed) {
        break;
      }
    }
  }

  private boolean onlyOneUncondBr(Function func) {
    boolean completed = true;

    for (var bbNode = func.getList_().getEntry().getNext(); bbNode != null; ) {
      var tmp = bbNode.getNext();
      var bb = bbNode.getVal();
      var brInst = (Instruction) (bb.getList().getLast().getVal());

      if (bb.getList().getNumNode() == 1 && brInst instanceof BrInst
          && brInst.getOperands().size() == 1) {
        boolean flag = false;
        var succ = (BasicBlock) (brInst.getOperands().get(0));
        if (succ.getList().getEntry().getVal() instanceof Phi) {
          for (var pred : bb.getPredecessor_()) {
            if (succ.getPredecessor_().contains(pred)) {
              flag = true;
            }
          }
        }

        if (flag) {
          bbNode = tmp;
          continue;
        }

        completed = false;

        // 维护 pred 的所有 br，维护 pred/succ 关系
        int bbIndex = succ.getPredecessor_().indexOf(bb);
        succ.getPredecessor_().remove(bb);

        for (var pred : bb.getPredecessor_()) {
          var predLastInst = (Instruction) (pred.getList().getLast().getVal());
          assert predLastInst instanceof BrInst;
          if (predLastInst.getNumOP() == 1) {
            predLastInst.CoSetOperand(0, succ);
            pred.getSuccessor_().set(0, succ);
          } else if (predLastInst.getNumOP() == 3) {
            for (int i = 1; i <= 2; i++) {
              if (predLastInst.getOperands().get(i) == bb) {
                predLastInst.CoSetOperand(i, succ);
                pred.getSuccessor_().set(i - 1, succ);
              }
            }
          }
          succ.getPredecessor_().add(pred);
        }

        // 维护 succ 的 phi
        for (var instNode : succ.getList()) {
          var inst = instNode.getVal();
          if (!(inst instanceof Phi)) {
            break;
          }

          Value valueFromBB = ((Phi) inst).getIncomingVals().get(bbIndex);
          ((Phi) inst).removeIncomingVals(bbIndex);
          for (var i = 0; i < bb.getPredecessor_().size(); i++) {
            inst.COaddOperand(valueFromBB);
          }
        }

        // 删掉 bb
        brInst.CORemoveAllOperand();
        bbNode.removeSelf();
      }

      bbNode = tmp;
    }

    return completed;
  }

  private boolean endWithUncondBr(Function func) {
    boolean completed = true;

    // bb 的 next 自动更新，这样迭代应该没问题？
    for (var bbNode : func.getList_()) {
      boolean continueOptThisBB = true;
      var bb = bbNode.getVal();
      while (continueOptThisBB) {
        continueOptThisBB = false;
        Instruction brInst = bbNode.getVal().getList().getLast().getVal();
        if (brInst instanceof BrInst && brInst.getNumOP() == 1) {
          continueOptThisBB = mergeBasicBlock(bb, (BasicBlock) brInst.getOperands().get(0));
          if (continueOptThisBB) {
            completed = false;
          }
        }
      }
    }

    return completed;
  }

  private boolean mergeBasicBlock(BasicBlock pred, BasicBlock succ) {
    assert pred.getList().getLast().getVal() instanceof BrInst;
    BrInst brInst = (BrInst) pred.getList().getLast().getVal();
    assert brInst.getNumOP() == 1;
    if (succ.getPredecessor_().size() != 1) {
      return false;
    }
    assert !(succ.getList().getEntry().getVal() instanceof Phi);

    for (var instNode = succ.getList().getEntry(); instNode != null; ) {
      var tmp = instNode.getNext();

      // 从 succ 中移除，放进 pred
      instNode.removeSelf();
      instNode.insertBefore(pred.getList().getLast());

      instNode = tmp;
    }
    // 删掉 pred 原有的 br，修改 pred
    pred.getList().getLast().removeSelf();
    // 维护 predecessor 和 successor 信息
    pred.setSuccessor_(succ.getSuccessor_());
    for (var bb : succ.getSuccessor_()) {
      int index = bb.getPredecessor_().indexOf(succ);
      assert index != -1;
      bb.getPredecessor_().set(index, pred);
      // phi 指令的操作数匹配 predecessor 的索引，所以不需要维护
    }

    // 直接从函数的 list 里删掉，应该没问题？
    succ.node_.removeSelf();
    return true;
  }

  private boolean removeDeadBB(Function func) {
    return true;
  }
}
