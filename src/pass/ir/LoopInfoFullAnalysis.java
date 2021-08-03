package pass.ir;

import ir.MyModule;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

// ! 在 BrOpt + GVNGCM 尽量化简后才能保证正确性
public class LoopInfoFullAnalysis implements IRPass {

  private final Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "loopInfoFullAnalysis";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopInfoFullAnalysis");

    for (var funcNode: m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        func.getLoopInfo().computeLoopInfo(func);
        func.getLoopInfo().computeAdditionalLoopInfo();
      }
    }
  }
}
