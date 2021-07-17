package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.TerminatorInst.BrInst;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Mylogger;

public class BBPredSucc implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "bbPredSucc";
  }


  @Override
  public void run(MyModule m) {
    log.info("Running pass: bbPredSucc");

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runBBPredSucc(func);
      }
    }
  }

  public void addEdge(BasicBlock pred, BasicBlock succ) {
    pred.getSuccessor_().add(succ);
    succ.getPredecessor_().add(pred);
  }

  public void runBBPredSucc(Function func) {
    for (var bbNode: func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      Instruction brInst = bb.getList().getLast().getVal();
      if (! (brInst instanceof BrInst)) {
        continue;
      }
      if (brInst.getNumOP() == 1) {
        addEdge(bb, (BasicBlock) brInst.getOperands().get(0));
      } else if (brInst.getNumOP() == 3) {
        addEdge(bb, (BasicBlock) brInst.getOperands().get(1));
        addEdge(bb, (BasicBlock) brInst.getOperands().get(2));
      }
    }
  }
}
